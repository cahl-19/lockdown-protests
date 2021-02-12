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
/**********************************************************************************************************************/
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
function load_protests(map) {
    let bounds = map.getBounds();

    let north = bounds._northEast.lat;
    let west = normalize_longitude(bounds._southWest.lng);
    let south = bounds._southWest.lat;
    let east = normalize_longitude(bounds._northEast.lng);

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
        () => {
            alert('error loading protests');
        }
    );
}
/**********************************************************************************************************************/
function config_map(map_div, api_token) {
    let map = L.map(map_div.attr('id')).setView([51.505, -0.09], 13);
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
        accessToken: 'your.mapbox.access.token'
        }
    ).addTo(map);

    load_protests(map);

    map.on('zoomend', () => {
        load_protests(map);
    });
    map.on('moveend', () => {
        load_protests(map);
    });

    return map;
}
/**********************************************************************************************************************/
export let protest_map = {

    'load_protests': load_protests,
    'denormalize_longitude': denormalize_longitude,
    'normalize_longitude': normalize_longitude,

    'mouse_event_outside_map': function(map, map_div, ev) {
        let point = map.mouseEventToContainerPoint(ev);

        if(point.x < 0 || point.y < 0) {
            return true;
        } else if(point.x > map_div.width() || point.y > map_div.height()) {
            return true;
        } else {
            return false;
        }
    },
    'init_map': function (map_div) {
        return new Promise((success, fail) => {
            api.call(
                '/api/test/map-api-token',
                'GET',
                {},
                (data) => {
                    success(config_map(map_div, data.token));
                },
                (status, err) => {
                    let description = err === undefined ? status : err.description;
                    error_map(`Error retrieving API token from server - ${description}`);
                    fail();
                }
            );
        });
    }
};
/**********************************************************************************************************************/
export default protest_map;
/**********************************************************************************************************************/
