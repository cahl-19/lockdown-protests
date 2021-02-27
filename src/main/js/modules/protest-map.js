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
import sanitize from 'sanitize'
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const PROTEST_LOAD_ZONE_BUFFER_FACTOR = 1.25;
const PROTEST_REFRESH_PERIOD = 60000;
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function error_map(map_div, error_message) {
    map_div.html(`<p>Error initializing map: ${error_message}</p>`);
}
/**********************************************************************************************************************/
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
/**********************************************************************************************************************/
function normalize_longitude(lng) {
        while(lng < -180) {
            lng += 360;
        }
        while(lng > 180) {
            lng -= 360;
        }
        return lng;
    }
/**********************************************************************************************************************/
function clamp_lattitude(lat) {
    if(lat > 90.0) {
        return 90.0;
    } else if(lat < -90.0) {
        return -90.0;
    } else {
        return lat;
    }
}
/**********************************************************************************************************************/
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
/**********************************************************************************************************************/
function time_to_refresh_pins(now, last_update) {
    return now > (last_update + PROTEST_REFRESH_PERIOD);
}
/**********************************************************************************************************************/
function buffer_bounds(bounds) {
    let span_ns = bounds.north - bounds.south;
    let span_ew = bounds.east - bounds.west;

    return {
        'north': clamp_lattitude(bounds.north + span_ns * PROTEST_LOAD_ZONE_BUFFER_FACTOR / 2),
        'west': normalize_longitude(bounds.west - span_ew * PROTEST_LOAD_ZONE_BUFFER_FACTOR / 2),
        'south': clamp_lattitude(bounds.south - span_ns * PROTEST_LOAD_ZONE_BUFFER_FACTOR / 2),
        'east': normalize_longitude(bounds.east + span_ew * PROTEST_LOAD_ZONE_BUFFER_FACTOR / 2)
    };
}
/**********************************************************************************************************************/
function update_protests(map, state) {
    let bounds = map.getBounds();
    let now = Date.now();

    let map_bounds = {
        'north': bounds._northEast.lat,
        'west': normalize_longitude(bounds._southWest.lng),
        'south': bounds._southWest.lat,
        'east': normalize_longitude(bounds._northEast.lng)
    };

    if(bounds_contained(state.protest_zone, map_bounds) && !time_to_refresh_pins(now, state.last_protest_update)) {
        return;
    } else {
        state.protest_zone = buffer_bounds(map_bounds);
        state.last_protest_update = now;
        load_protests(map, state.protest_zone, state.config.display_error);
    }
}
/**********************************************************************************************************************/
function load_protests(map, bounds, display_error) {

    let north = bounds.north;
    let west = bounds.west;
    let south = bounds.south;
    let east = bounds.east;

    if(display_error === undefined) {
        display_error = (alert) => alert('error loading protests');
    }

    api.call(
        `/api/pins`,
        'GET',
        {'SW': `${south},${west}`, 'NE': `${north},${east}`},
        (data) => {
            data.protests.forEach((protest) => {

                let lat = protest.location.latitude;
                let lng = denormalize_longitude(
                    protest.location.longitude, map
                );

                let title = sanitize.encode_api_html(protest.title);
                let owner = sanitize.encode_api_html(protest.owner);
                let description = sanitize.encode_api_html(protest.description);
                let dress_code = sanitize.encode_api_html(protest.dressCode);
                let date = new Date(protest.date);

                let dt = new Intl.DateTimeFormat([], { dateStyle: 'full', timeStyle: 'long' }).format(date);

                let mark = L.marker([lat, lng]).addTo(map);
                mark.bindPopup(
                    `<p><strong>${title}</strong> - by ${owner}</p>` +
                    `<p><strong>Scheduled for:</strong> ${dt}</p>` +
                    `<p><strong>Dresss Code:</strong> ${dress_code} </p>` +
                    `<p><strong>Description: </strong></br>${description}</p>`
                );
            });
        },
        (status, error_body) => {
            display_error(status, error_body);
        }
    );
}
/**********************************************************************************************************************/
function config_map(map_div, api_token, config) {

    let state = {
        'config': config,
        'protest_zone': undefined,
        'last_protest_update': Number.NEGATIVE_INFINITY
    };
    let map = L.map(
        map_div.attr('id'),
        {
            tap: false
        }
    ).setView([51.505, -0.09], 13);

    map.on('locationerror', ()=> map.setView([51.505, -0.09], 13));
    map.locate({setView: true, maxZoom: 29});


    let url = `https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=${api_token}`;

    L.Icon.Default.imagePath = 'assets/leaflet/';

    map.setMinZoom(1);

    L.tileLayer(url, {
        attribution: (
            'Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, ' +
            'Imagery © <a href="https://www.mapbox.com/">Mapbox</a>')
        ,
        maxZoom: 18,
        id: 'mapbox/streets-v11',
        tileSize: 512,
        zoomOffset: -1,
        accessToken: 'your.mapbox.access.token',
        }
    ).addTo(map);

    update_protests(map, state);

    map.on('zoomend', () => {
        update_protests(map, state);
    });
    map.on('moveend', () => {
        update_protests(map, state);
    });

    return map;
}
/**********************************************************************************************************************/
export let protest_map = {

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
                '/api/test/map-api-token',
                'GET',
                {},
                (data) => {
                    success(config_map(map_div, data.token, config));
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
/**********************************************************************************************************************/
export default protest_map;
/**********************************************************************************************************************/
