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

import '!style-loader!css-loader!bootstrap/dist/css/bootstrap.min.css';
import '!style-loader!css-loader?url=false!leaflet/dist/leaflet.css';

import Popper from 'popper.js';
import 'bootstrap';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function reposition_mouse_event(ev, x_adjust, y_adjust) {
    ev.pageX -= x_adjust;
    ev.offsetX -= x_adjust;
    ev.clientX -= x_adjust;
    ev.screenX -= x_adjust;

    ev.pageY -= y_adjust;
    ev.offsetY -= y_adjust;
    ev.clientY -= y_adjust;
    ev.screenY -= y_adjust;
}
/**********************************************************************************************************************/
function mouse_event_outside_map(map, ev) {
    let point = map.mouseEventToContainerPoint(ev);
    let mapdiv = $('#mapdiv');

    if(point.x < 0 || point.y < 0) {
        return true;
    } else if(point.x > mapdiv.width() || point.y > mapdiv.height()) {
        return true;
    } else {
        return false;
    }
}
/**********************************************************************************************************************/
function setup_click_drag_pin(map) {

    let pin = $('#droppable-pin');
    let modal = $('#protest-form-modal');

    let pin_orig = pin.offset();

    let state = {
        'modal_open': false,
        'pin_reset': true,
    };

    function restore_pin() {
        pin.animate(
            {'top': pin_orig.top, 'left': pin_orig.left},
            400,
            "swing",
            () => {
                pin.css('position', 'static');
                pin.css('left', '');
                pin.css('top', '');
                pin.css('z-index', 0);
                pin.css('cursor', 'grab');

                state.pin_reset = true;
            }
        );
    };

    pin.on('mousedown touchstart', (mousedown_ev) => {
        if(state.modal_open || !state.pin_reset) {
            return;
        }

        state.pin_reset = false;
        state.over_map = false;

        pin.css('position', 'fixed');
        pin.css('z-index', 1040);

        let offsetX = mousedown_ev.offsetX;
        let offsetY = mousedown_ev.offsetY;

        pin.css('cursor', 'grabbing');

        $(document).on('mousemove.pindrag touchmove.pindrag', (mousemove_ev) => {

            let x = mousemove_ev.pageX - offsetX;
            let y = mousemove_ev.pageY - offsetY;

            pin.css('left', x);
            pin.css('top', y);
        });
        $(document).on('mouseup.pindrag touchend.pindrag', (mouseup_ev) => {

            $(document).off('mousemove.pindrag touchmove.pindrag');
            $(document).off('mouseup.pindrag touchend.pindrag');

            pin.css('cursor', 'auto');

            reposition_mouse_event(mouseup_ev, offsetX - pin.width() / 2, offsetY - pin.height());

            if(mouse_event_outside_map(map, mouseup_ev)) {
                restore_pin();
                return;
            }

            let position = map.mouseEventToLatLng(mouseup_ev);

            $('#display-protest-plan-location').text(
                `Lat: ${position.lat.toFixed(3)}, Long: ${normalize_longitude(position.lng.toFixed(3))}`
            );
            $('#protest-latitude').val(position.lat);
            $('#protest-longitude').val(position.lng);

            state.modal_open = true;
            modal.modal('show');
        });
    });

    modal.on('hidden.bs.modal', () => {
        state.modal_open = false;
        restore_pin();
    });
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
function denormalize_longitude(lng, lngCenter) {
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
                    protest.location.longitude, map.getCenter().lng
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
function display_map_bounds(map) {
    let bounds = map.getBounds();

    let north = bounds._northEast.lat;
    let west = normalize_longitude(bounds._southWest.lng);
    let south = bounds._southWest.lat;
    let east = normalize_longitude(bounds._northEast.lng);

    $('#map-info').html(
        `<p><strong>Map Bounds:</strong></p>` +
        `<p>South West: ${south.toFixed(2)}, ${west.toFixed(2)}</br>` +
        `North East: ${north.toFixed(2)}, ${east.toFixed(2)}</p>`
    );
}
/**********************************************************************************************************************/
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

    mymap.setMinZoom(1);

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

    display_map_bounds(mymap);
    load_protests(mymap);

    mymap.on('zoomend', () => {
        display_map_bounds(mymap);
        load_protests(mymap);
    });
    mymap.on('moveend', () => {
        display_map_bounds(mymap);
        load_protests(mymap);
    });

    setup_click_drag_pin(mymap);
}
/**********************************************************************************************************************/
function setup_sidebar() {

    let chat_height = 0;
    $('.sidebar > .chat-card').each(function() {
        chat_height += $(this).outerHeight();
    });


    if(api.whoami() !== undefined) {
        $('#pin-card').removeClass('hidden');
    }

    let pin_card_height = $('.pin-card').outerHeight();
    let pad = $('.sidebar').outerHeight() - $('.sidebar').innerHeight();

    $('.sidebar-spacer').css('height', `calc(100% - ${chat_height + pin_card_height  + pad + 15}px`);
}
/**********************************************************************************************************************/
function date_from_inputs(date, time) {
        let dval = date.val();
        let tval = time.val();

        if(!dval || !tval) {
            return undefined;
        }

        let date_parts = dval.split('-');
        let time_parts = tval.split(':');

        return new Date(
                date_parts[0], date_parts[1] - 1, date_parts[2], time_parts[0], time_parts[1]
        );
}
/**********************************************************************************************************************/
function setup_protest_form() {

    let form = $('#protest-form');
    let submit_button = $('#submit-new-protest');

    let title = $('#protest-title');
    let dress_code = $('#protest-dress-code');
    let description = $('#protest-description');
    let date = $('#protest-date');
    let time = $('#protest-time');
    let dt_validity_feedback = $('#protest-date-time-validity-feedback');
    let modal = $('#protest-form-modal');

    title.attr('maxlength', 256);
    title.attr('minlength', 4);

    description.attr('maxlength', 768);
    description.attr('minlength', 8);

    dress_code.attr('maxlength', 128);

    window.setInterval(() => {
        let dt = new Intl.DateTimeFormat([], { dateStyle: 'full', timeStyle: 'long' }).format(Date.now());
        $('#protest-user-current-time').text(dt);
    }, 1000);

    function validate_date() {

        let dt = date_from_inputs(date, time);

        if(dt === undefined) {
            let msg = 'Date & time are required';
            dt_validity_feedback.text(msg);
            date[0].setCustomValidity(msg);
            return;
        }

        let now = new Date(Date.now());

        if(dt.getTime() < now.getTime()) {
            let msg = 'Must be set in future.';
            dt_validity_feedback.text(msg);
            date[0].setCustomValidity(msg);
        } else {
            date[0].setCustomValidity('');
        }
    };

    date.on('input', validate_date);
    time.on('input', validate_date);

    submit_button.on('click', () => form.submit());

    form.submit((ev) => {
        ev.preventDefault();
        ev.stopPropagation();

        validate_date();

        let valid = form[0].checkValidity();
        form.addClass('was-validated');

        if(!valid) {
            return;
        }

        let username = api.whoami();

        if(username === undefined) {
            return;
        }

        let protest = {
            'location': {
                'latitude': $('#protest-latitude').val(), 'longitude': $('#protest-longitude').val()
            },
            'owner': username,
            'title': title.val(),
            'description': description.val(),
            'dressCode': dress_code.val(),
            'date': date_from_inputs(date, time).getTime()
        };

        api.call(
            '/api/pins', 'POST', protest, () => modal.modal('hide'), () => alert('failure')
        );

    });
}
/**********************************************************************************************************************/
function setup() {
    setup_map();
    setup_sidebar();
    setup_protest_form();
}
/**********************************************************************************************************************/
function error_map(error_message) {
    $('#mapdiv').html(`<p>Error initializing map: ${error_message}</p>`);
}
/**********************************************************************************************************************/
$(document).ready(function() {
    api.clean_dead_sessions().then(setup);
});
/**********************************************************************************************************************/
