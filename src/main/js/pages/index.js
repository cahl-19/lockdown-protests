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
import protest_map from 'protest-map';
import api from 'api';
import sanitize from 'sanitize'
import display_error from 'display-error';

import '!style-loader!css-loader!bootstrap/dist/css/bootstrap.min.css';
import '!style-loader!css-loader?url=false!leaflet/dist/leaflet.css';

import Popper from 'popper.js';
import 'bootstrap';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function popup_ajax_error(status, error_body) {
    let error_popup = $('#error-popup');

    if(error_body === undefined && status === 0){
        return display_error.display(error_popup, 'Unable to complete request, lost connection to server.');
    } else if(error_body !== undefined) {
        return display_error.display(error_popup, `Error: ${status} - ${error.description}.`);
    } else {
        return display_error.display('Error completing request.');
    }
}
/**********************************************************************************************************************/
function make_spinner() {
    return $('<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>');
}
/**********************************************************************************************************************/
function event_client_xy(ev) {
    if (ev.touches !== undefined) {
        return {'x': ev.touches[0].clientX, 'y': ev.touches[0].clientY};
    } else {
        return {'x': ev.clientX, 'y': ev.clientY};
    }
}
/**********************************************************************************************************************/
function event_offset_xy(ev) {
    if (ev.touches !== undefined) {
        let rect = ev.target.getBoundingClientRect();
        return {'x': ev.targetTouches[0].pageX - rect.left, 'y': ev.targetTouches[0].pageY - rect.top};
    } else {
        return {'x': ev.offsetX, 'y': ev.offsetY};
    }
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
function setup_click_drag_pins(map) {
    let desktop_pin = $('#droppable-pin');
    let mobile_pin  = $('#mobile-droppable-pin');

    let mobile_pin_parent = mobile_pin.parent();

    let protest_form_modal = $('#protest-form-modal');
    let mobile_pin_modal = $('#pin-drop-modal');

    activate_drop_pin(map, desktop_pin, protest_form_modal);

    activate_drop_pin(
        map, mobile_pin, protest_form_modal,
        (pin) => {
            pin.detach();
            $(document.body).append(mobile_pin);
            mobile_pin_modal.modal('hide');
        },
        (pin) => {
            pin.detach();
            mobile_pin_parent.append(mobile_pin);
        }
    );
}
/**********************************************************************************************************************/
function activate_drop_pin(map, pin, protest_form_modal, on_grab, on_reset) {

    if(on_grab === undefined) {
        on_grab = () => {};
    }
    if(on_reset === undefined) {
        on_reset = () => {};
    }

    let state = {
        'modal_open': false,
        'pin_reset': true,
        'pin_orig': pin.offset()
    };

    function restore_pin() {
        pin.animate(
            {'top': state.pin_orig.top, 'left': state.pin_orig.left},
            400,
            "swing",
            () => {
                pin.css('position', 'static');
                pin.css('left', '');
                pin.css('top', '');
                pin.css('z-index', 'auto');
                pin.css('cursor', 'grab');

                state.pin_reset = true;
                on_reset(pin);
            }
        );
    };

    pin.on('mousedown touchstart', (mousedown_ev) => {

        mousedown_ev.preventDefault();

        if(state.modal_open || !state.pin_reset) {
            return;
        }

        state.pin_reset = false;
        state.over_map = false;
        state.pin_orig = pin.offset();

        let offsetX = event_offset_xy(mousedown_ev).x;
        let offsetY = event_offset_xy(mousedown_ev).y;

        pin.css('position', 'fixed');
        pin.css('z-index', 1040);
        pin.css('left', event_client_xy(mousedown_ev).x - offsetX);
        pin.css('top', event_client_xy(mousedown_ev).y - offsetY);

        pin.css('cursor', 'grabbing');

        on_grab(pin);

        $(document.body).on('mousemove.pindrag touchmove.pindrag', (mousemove_ev) => {
            mousemove_ev.preventDefault();

            pin.css('left', event_client_xy(mousemove_ev).x - offsetX);
            pin.css('top', event_client_xy(mousemove_ev).y - offsetY);
        });
        $(document.body).on('mouseup.pindrag touchend.pindrag', (mouseup_ev) => {
            mouseup_ev.preventDefault();

            $(document.body).off('mousemove.pindrag touchmove.pindrag');
            $(document.body).off('mouseup.pindrag touchend.pindrag');
            $(document.body).off('touchcancel.pindrag');

            if(mouseup_ev.type === 'mouseup') {
                pin.css('left', event_client_xy(mouseup_ev).x - offsetX);
                pin.css('top', event_client_xy(mouseup_ev).y - offsetY);
            }

            let x = pin.position().left;
            let y = pin.position().top;

            pin.css('cursor', 'auto');

            if(protest_map.xy_outside_map(map, $('#map-div'), x, y)) {
                restore_pin();
                return;
            }

            let position = protest_map.xy_to_latlng(
                    map, x + pin.width() / 2, y + pin.height()
            );

            $('#display-protest-plan-location').text(
                `Lat: ${position.lat.toFixed(3)}, Long: ${protest_map.normalize_longitude(position.lng.toFixed(3))}`
            );
            $('#protest-latitude').val(position.lat);
            $('#protest-longitude').val(position.lng);

            state.modal_open = true;
            protest_form_modal.modal('show');
        });
        $(document.body).on('touchcancel.pindrag', () => {

            $(document).off('mousemove.pindrag touchmove.pindrag');
            $(document).off('mouseup.pindrag touchend.pindrag');
            $(document).off('touchcancel.pindrag');

            restore_pin();
        });
    });

    protest_form_modal.on('hidden.bs.modal', () => {
        state.modal_open = false;
        restore_pin();
    });
}
/**********************************************************************************************************************/
function setup_sidebar() {

    if(api.whoami() === undefined) {
        return;
    }

    $('#pin-card').removeClass('hidden');
    $('#notification-card').removeClass('hidden');
}
/**********************************************************************************************************************/
function setup_menu() {
    if(api.whoami() === undefined) {
        $('#login-menu-item').removeClass('hidden');
        $('#register-menu-item').removeClass('hidden');
    } else {
        let pin_drop_menu_item = $('#drop-pin-menu-item');

        pin_drop_menu_item.removeClass('hidden');
        pin_drop_menu_item.on('click', () => $('#pin-drop-modal').modal('show'));
    }
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
            '/api/pins',
            'POST',
            protest,
            () => window.location.reload(true),
            (status, error) => popup_ajax_error(status, error)
        );
    });
}
/**********************************************************************************************************************/
function setup_login() {

    let submit_login = $('#submit-login');
    let email_input = $('#email-input');
    let password_input = $('#password-input');
    let form = $('#login-form');

    if(api.whoami() !== undefined) {
        return;
    }

    $('#login-card').removeClass('hidden');

    $('#login-button,#login-menu-item').on('click', () => {
        $('#login-modal').modal('show');
    });

        submit_login.on('click', (ev) => {
        ev.preventDefault();

        let orig_button_content = submit_login.html();
        let spinner = make_spinner();

        submit_login.html('');

        submit_login.append(spinner);
        submit_login.append($('<span> Loading</span>'));

        api.login(
            email_input.val(), password_input.val(),
            () => {
                window.location.reload(true);
            },
            (status, error) => {
                submit_login.html(orig_button_content);

                if(error !== undefined && error.code === api.error_codes.LOGIN_FAILURE) {
                    password_input[0].setCustomValidity('Invalid username or password');
                    form[0].checkValidity();
                    form.addClass('was-validated');
                } else {
                    popup_ajax_error(status, error);
                }
            }
        );
    });
}
/**********************************************************************************************************************/
function setup_auth_page() {

    protest_map.init_map(
        $('#map-div'), {
            'display_error': (status, error) => popup_ajax_error(status, error).then(() => window.location.reload(true))
        }
    ).then((map) => {
        setup_click_drag_pins(map);
    });

    setup_sidebar();
    setup_menu();
    setup_login();
}
/**********************************************************************************************************************/
function setup_unauth_page() {
    setup_protest_form();
}
/**********************************************************************************************************************/
$(document).ready(function() {
    setup_unauth_page();
    api.clean_dead_sessions().then(setup_auth_page);
});
/**********************************************************************************************************************/
