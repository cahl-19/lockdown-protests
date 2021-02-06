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

import Popper from 'popper.js';
import 'bootstrap';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function setup_map() {
    api.call(
        '/api/test/map-api-token',
        'GET',
        {},
        (data) => {
            init_map(data.token);
        },
        (status, err) => {
            let description = err === undefined ? status : err.description;
            error_map(`Error retrieving API token from server - ${description}`);
        }
    );
}
/**********************************************************************************************************************/
function init_map(api_token) {

    let mymap = L.map('mapdiv').setView([51.505, -0.09], 13);
    let url = `https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=${api_token}`;

    L.Icon.Default.imagePath = 'assets/leaflet/';

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
    ).addTo(mymap);

    let marker = L.marker([51.5, -0.09]).addTo(mymap);

    marker.bindPopup("<b>Lockdown Protest at 18:00:00 UTC</b><br>Be, there, dress in black.").openPopup();

    mymap.on('click', (ev) => {
        let username = api.whoami();
        let mark = L.marker([ev.latlng.lat, ev.latlng.lng]).addTo(mymap);
        mark.bindPopup(`<p>Lat: ${ev.latlng.lat}</p><p>Long: ${ev.latlng.lng}</p>`).openPopup();

        if(username === undefined) {
            return;
        }

        let protest = {
            'location': {'latitude': ev.latlng.lat, 'longitude': ev.latlng.lng},
            'owner': username,
            'title': 'Test-Protest',
            'description': 'test description <script></script>',
            'dressCode': 'Neked',
            'date': 0
        };

        api.call('/api/pins', 'POST', protest, () => undefined, () => alert('failure'));
    });
}
/**********************************************************************************************************************/
function error_map(error_message) {
    $('#mapdiv').html(`<p>Error initializing map: ${error_message}</p>`);
}
/**********************************************************************************************************************/
$(document).ready(function() {
   setup_map();
});
/**********************************************************************************************************************/
