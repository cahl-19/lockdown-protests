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
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function setup_map() {

        $.ajax({
            'type': "GET",
            'url': "/api/test/map-api-token",
            'dataType': "json",

            'success': (data) => {
                if(data.token) {
                    init_map(data.token);
                } else {
                    error_map('No API token configured.');
                }
            },

            'error': (msg) => {
                error_map(`Error retrieving API token from server - ${msg}`);
            }
        });
}
/**********************************************************************************************************************/
function init_map(api_token) {
    let mymap = L.map('mapdiv').setView([51.505, -0.09], 13);
    let url = `https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token=${api_token}`

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