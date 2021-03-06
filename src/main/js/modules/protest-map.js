/*
 * This File is Part of LDProtest
 * Copyright (C) 2021 Covid Anti Hysterics League
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/***********************************************************************************************************************
*                                                       IMPORTS                                                        *
***********************************************************************************************************************/
import $ from 'jquery';
import L from 'leaflet';
import api from 'api';
import sanitize from 'sanitize';
import protest_object from 'protest-object';
import geocode from 'geocode';
import util from 'ldp-util';
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const PROTEST_LOAD_ZONE_BUFFER_FACTOR = 1.25;
const PROTEST_REFRESH_PERIOD = 60000;
const POPUP_MAX_WIDTH_PIXELS = 500;

const DEFAULT_INITIAL_ZOOM = 13;
const INITIAL_ZOOM_IF_LOCATION_DISABLED = 10;

const DEFAULT_INITIAL_LATITUDE = 51.505;
const DEFAULT_INITIAL_LONGITUDE = -0.09;
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function initial_view_port(config) {
    let params = new URLSearchParams(window.location.search);

    let i_lat = util.strict_parse_float(params.get('ilat'));
    let i_long = util.strict_parse_float(params.get('ilong'));

    let i_zoom = util.default_if_undefined(util.strict_parse_int(params.get('izoom')), DEFAULT_INITIAL_ZOOM);

    if(i_lat !== undefined && i_long !== undefined) {
        return {
            'latitude': i_lat,
            'longitude': i_long,
            'zoom': i_zoom
        };
    } else if(config.geoIpLocation !== undefined) {
        let geo_ip = config.geoIpLocation;
        return {
            'latitude': util.default_if_undefined(geo_ip.latitude, DEFAULT_INITIAL_LATITUDE),
            'longitude': util.default_if_undefined(geo_ip.longitude, DEFAULT_INITIAL_LONGITUDE),
            'zoom': i_zoom
        };
    } else {
        return {
            'latitude': DEFAULT_INITIAL_LATITUDE,
            'longitude': DEFAULT_INITIAL_LONGITUDE,
            'zoom': i_zoom
        };
    }
}

function popup_max_width_pixels() {
    return Math.min(0.8 * $(window).width(), POPUP_MAX_WIDTH_PIXELS)
}

function error_map(map_div, error_message) {
    map_div.html(`<p>Error initializing map: ${error_message}</p>`);
}

function denormalize_longitude(lng, map) {

    let lngCenter = map.getCenter().lng;

    while(Math.abs(lngCenter - lng) > 180.0) {
        if(lng < lngCenter) {
            lng += 360.0;
        } else {
            lng -= 360.0;
        }
    }

    return lng;
}

function normalize_longitude(lng) {
    while(lng < -180) {
        lng += 360;
    }
    while(lng > 180) {
        lng -= 360;
    }
    return lng;
}

function normalize_bounds(bounds) {

    if((bounds.east - bounds.west) >= 360) {
        return {'west': -180.0, 'east': 180.0, 'north': bounds.north, 'south': bounds.south};
    }

    return {
        'west': normalize_longitude(bounds.west),
        'east': normalize_longitude(bounds.east),
        'north': bounds.north,
        'south': bounds.south
    };
}

function clamp_latitude(lat) {
    if(lat > 90.0) {
        return 90.0;
    } else if(lat < -90.0) {
        return -90.0;
    } else {
        return lat;
    }
}

function bounds_contained(container, bounds) {
    if(container === undefined) {
        return false;
    } else if(bounds.north > container.north) {
        return false;
    } else if(bounds.south < container.south) {
        return false;
    } else if(bounds.east > container.east) {
        return false;
    } else if(bounds.west < container.west) {
        return false;
    } else {
        return true;
    }
}

function time_to_refresh_pins(now, last_update) {
    return now > (last_update + PROTEST_REFRESH_PERIOD);
}

function buffer_bounds(bounds) {
    let span_ns = bounds.north - bounds.south;
    let span_ew = bounds.east - bounds.west;

    let ew_adj = span_ew * PROTEST_LOAD_ZONE_BUFFER_FACTOR / 2;
    let ns_adj = span_ns * PROTEST_LOAD_ZONE_BUFFER_FACTOR / 2;

    return {
        'north': clamp_latitude(bounds.north + ns_adj),
        'west': bounds.west - ew_adj,
        'south': clamp_latitude(bounds.south - ns_adj),
        'east': bounds.east + ew_adj
    };
}

function update_api_token(map_div, title_layer, search) {
    api.call(
        '/api/map-api-token',
        'GET',
        {},
        (data) => {
            if(data.token) {
                title_layer.setUrl(
                    `https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=${data.token}`
                );
                search.change_token(data.token);
            } else {
                error_map(map_div, `Unable to fetch API token for map redraw.`);
                fail();
            }
        },
        (status, err) => {
            let description = err === undefined ? status : err.description;
            error_map(map_div, `Unable to fetch API token for map redraw - ${description}`);
            fail();
        }
    );
}

function update_protests(map, state) {
    let bounds = map.getBounds();
    let now = Date.now();

    let map_bounds = {
        'north': bounds._northEast.lat,
        'west': bounds._southWest.lng,
        'south': bounds._southWest.lat,
        'east': bounds._northEast.lng
    };

    if(bounds_contained(state.protest_zone, map_bounds) && !time_to_refresh_pins(now, state.last_protest_update)) {
        return;
    } else {
        state.protest_zone = buffer_bounds(map_bounds);
        state.last_protest_update = now;
        load_protests(map, normalize_bounds(state.protest_zone), state);
    }
}

function load_protests(map, bounds, state) {

    let north = bounds.north;
    let west = bounds.west;
    let south = bounds.south;
    let east = bounds.east;

    api.call(
        `/api/pins`,
        'GET',
        {'SW': `${south},${west}`, 'NE': `${north},${east}`},
        (data) => {

            let old_markers = state.markers;

            old_markers.forEach((mark) => {
                map.removeLayer(mark);
            });

            old_markers.splice(0, old_markers.length);

            data.protests.forEach((protest) => {

                let lat = protest.location.latitude;
                let lng = denormalize_longitude(
                    protest.location.longitude, map
                );

                let protest_obj = new protest_object.Protest(protest);
                let icon = state.config.choose_marker_icon(protest_obj);

                let mark = icon === undefined ?
                    L.marker([lat, lng]).addTo(map) :
                    L.marker([lat, lng], {'icon': L.icon(icon)}).addTo(map);

                mark.bindPopup(() => state.config.render_popup(protest_obj), {
                    'maxWidth' : popup_max_width_pixels()
                });

                state.markers.push(mark);
            });
        },
        (status, error_body) => {
            state.config.display_error(status, error_body);
        }
    );
}

function config_map(map_div, initial_api_token, config) {

    let state = {
        'config': config,
        'protest_zone': undefined,
        'last_protest_update': Number.NEGATIVE_INFINITY,
        'markers': []
    };

    let init_vp = initial_view_port(config);

    let initial_latitude = init_vp.latitude;
    let initial_longitude = init_vp.longitude;
    let initial_zoom = init_vp.zoom;

    let map = L.map(
        map_div.attr('id'),
        {
            tap: false
        }
    ).setView([initial_latitude, initial_longitude], initial_zoom);

    map.on('locationerror', ()=> map.setView([initial_latitude, initial_longitude], INITIAL_ZOOM_IF_LOCATION_DISABLED));
    map.locate({setView: true, maxZoom: 29});

    let url = `https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=${initial_api_token}`;

    L.Icon.Default.imagePath = 'assets/leaflet/';
    map.setMinZoom(4);

    let title_layer = L.tileLayer(url, {
        attribution: (
            'Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, ' +
            'Imagery © <a href="https://www.mapbox.com/">Mapbox</a>'
        ),
        maxZoom: 18,
        id: 'mapbox/streets-v11',
        tileSize: 512,
        zoomOffset: -1,
        accessToken: 'your.mapbox.access.token',
        }
    ).addTo(map);

    let search = geocode.search(
        map_div, map, {'token': initial_api_token, 'display_error': config.display_error}
    );
    search.start();

    update_protests(map, state);

    map.on('zoomend', () => {
        update_protests(map, state);
    });
    map.on('moveend', () => {
        update_protests(map, state);
    });

    window.setInterval(() => {
        update_protests(map, state);
    }, PROTEST_REFRESH_PERIOD / 2);

    if(CLIENT_CONFIG.MAP_API_TOKEN_REFRESH_SECONDS) {
        window.setInterval(
            () => update_api_token(map_div, title_layer, search), CLIENT_CONFIG.MAP_API_TOKEN_REFRESH_SECONDS * 1000
        );
    }

    return map;
}

export let protest_map = {
    'popup_max_width_pixels': popup_max_width_pixels,
    'load_protests': load_protests,
    'denormalize_longitude': denormalize_longitude,
    'normalize_longitude': normalize_longitude,

    'xy_to_latlng': function(map, x, y) {
        return map.mouseEventToLatLng({'clientX': x, 'clientY': y});
    },
    'xy_outside_map': function(map, map_div, x, y) {
        let point = map.mouseEventToContainerPoint({'clientX': x, 'clientY': y});

        if(point.x < 0 || point.y < 0) {
            return true;
        } else if(point.x > map_div.width() || point.y > map_div.height()) {
            return true;
        } else {
            return false;
        }
    },
    'init_map': function (map_div, config) {

        if(config === undefined) {
            config = {};
        }

        return new Promise((success, fail) => {
            api.call(
                '/api/map-config',
                'GET',
                {},
                (data) => {
                    if(data.token) {
                        config.geoIpLocation = data.geoIpLocation;
                        success(config_map(map_div, data.token, config));
                    } else {
                        error_map(map_div, `No API token defined.`);
                        fail();
                    }
                },
                (status, err) => {
                    let description = err === undefined ? status : err.description;
                    error_map(map_div, `Error retrieving API token from server - ${description}`);
                    fail();
                }
            );
        });
    }
};

export default protest_map;

