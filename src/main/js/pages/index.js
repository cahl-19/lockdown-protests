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
import protest_form from 'protest-form';
import api from 'api';
import sanitize from 'sanitize';
import display_error from 'display-error';
import spinner from 'spinner';
import confirm from 'confirm';

import '!style-loader!css-loader!bootstrap/dist/css/bootstrap.min.css';
import '!style-loader!css-loader?url=false!leaflet/dist/leaflet.css';

import Popper from 'popper.js';
import 'bootstrap';
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const PROTEST_CREATE_FORM_PREFIX = 'protest-create';
const PROTEST_EDIT_FORM_PREFIX = 'protest-edit';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function popup_ajax_error(status, error_body) {
    let error_popup = $('#error-popup');

    if(error_body === undefined && status === 0){
        return display_error.display(error_popup, 'Unable to complete request, lost connection to server.');
    } else if(error_body !== undefined) {
        return display_error.display(error_popup, `Error: ${status} - ${error_body.description}.`);
    } else {
        return display_error.display(error_popup, 'Error completing request.');
    }
}
/**********************************************************************************************************************/
function can_edit_protest(protest) {
    return (
        api.whoami() === protest.owner ||
        api.current_user_role() === 'ADMIN' ||
        api.current_user_role() === 'MODERATOR'
    );
}
/**********************************************************************************************************************/
function render_popup(protest) {
    let dt = new Intl.DateTimeFormat([], { dateStyle: 'full', timeStyle: 'long' }).format(protest.date);

    let content = $(
        '<div class="protest-popup">' +
            '<div>' +
            `<p><strong>${protest.title}</strong> - by ${protest.owner}</p>` +
            `<p><strong>Scheduled for:</strong> ${dt}</p>` +
            `<p><strong>Dresss Code:</strong> ${protest.dressCode} </p>` +
            `<p><strong>Description: </strong></br>${protest.description}</p>` +
            '</div>' +
        '</div>'
    );

    if(can_edit_protest(protest)) {
        let edit_row = $('<div class="text-right"></div>');
        let edit_link = $('<a href="#">Edit</a>');

        edit_link.on(
            'click', () => protest_form.open_form(
                PROTEST_EDIT_FORM_PREFIX, {
                    'title': sanitize.decode_api_html(protest.title),
                    'owner': sanitize.decode_api_html(protest.owner),
                    'description': sanitize.decode_api_html(protest.description),
                    'dressCode': sanitize.decode_api_html(protest.dressCode),
                    'date': protest.date,
                    'location': protest.location,
                    'protestId': protest.protestId
                }
            )
        );

        edit_row.append(edit_link);
        content.append(edit_row);
    }

    return content[0];
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
function setup_click_drag_pins(map) {
    let desktop_pin = $('#droppable-pin');
    let mobile_pin  = $('#mobile-droppable-pin');

    let mobile_pin_parent = mobile_pin.parent();

    let mobile_pin_modal = $('#pin-drop-modal');

    activate_drop_pin(map, desktop_pin);

    activate_drop_pin(
        map, mobile_pin,
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
function activate_drop_pin(map, pin, on_grab, on_reset) {

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

            protest_form.open_form(
                PROTEST_CREATE_FORM_PREFIX,
                {
                    'location': {
                        'latitude': position.lat,
                        'longitude': protest_map.normalize_longitude(position.lng)
                    }
                }
            );
            state.modal_open = true;
        });
        $(document.body).on('touchcancel.pindrag', () => {

            $(document).off('mousemove.pindrag touchmove.pindrag');
            $(document).off('mouseup.pindrag touchend.pindrag');
            $(document).off('touchcancel.pindrag');

            restore_pin();
        });
    });

    protest_form.on_hide(PROTEST_CREATE_FORM_PREFIX, () => {
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
        $('.dropdown-item-unauth').removeClass('hidden');
    } else {
        $('.dropdown-item-auth').removeClass('hidden');

        let pin_drop_menu_item = $('#drop-pin-menu-item');
        pin_drop_menu_item.on('click', () => $('#pin-drop-modal').modal('show'));
    }
}
/**********************************************************************************************************************/
function setup_registration() {
    let register_button = $('.registration-link');
    let register_modal = $('#registration-modal');
    let contributing_link = $('.contributing-link');
    let contributing_modal = $('#contributing-modal');

    register_button.on('click', () => register_modal.modal('show'));

    contributing_link.on('click', (ev) => {
        ev.preventDefault();
        register_modal.modal('hide');
        contributing_modal.modal('show');
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

        let stop_spin = spinner.spin(submit_login, ' Loading');

        api.login(
            email_input.val(), password_input.val(),
            () => {
                window.location.reload(true);
            },
            (status, error) => {
                stop_spin();

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
function setup_protest_forms() {

    protest_form.setup_form(
        PROTEST_CREATE_FORM_PREFIX,
        (protest, submit_button) => {
            let stop_spin = spinner.spin(submit_button, ' Loading');
            api.call(
                '/api/pins',
                'POST',
                protest,
                () => window.location.reload(true),
                (status, error) => {
                    stop_spin();
                    popup_ajax_error(status, error);
                }
            );
        }
    );

    protest_form.setup_form(
        PROTEST_EDIT_FORM_PREFIX,
        (protest, submit_button) => {
            let stop_spin = spinner.spin(submit_button, ' Loading');
            api.call(
                `/api/protests/${protest.protestId}`,
                'POST',
                protest,
                () => window.location.reload(true),
                (status, error) => {
                    stop_spin();
                    popup_ajax_error(status, error);
                }
            );
        },
        (protest, delete_button) => {
            let stop_spin = spinner.spin(delete_button, ' Loading');

            confirm.display(
                    $('#confirm-popup'), "Are you sure you want to delete this protest?\nThis cannot be undone."
            ).then(
                () => {
                    api.call(
                        `/api/protests/${protest.protestId}`,
                        'DELETE',
                        {},
                        () => window.location.reload(true),
                        (status, error) => {
                            stop_spin();
                            popup_ajax_error(status, error);
                        }
                    );
                },
                () => {
                    stop_spin();
                }
            );
        }
    );
}
/**********************************************************************************************************************/
function setup_auth_page() {

    protest_map.init_map(
        $('#map-div'), {
            'render_popup': (protest) => render_popup(protest),
            'display_error': (status, error) => popup_ajax_error(status, error).then(() => window.location.reload(true))
        }
    ).then((map) => {
        setup_click_drag_pins(map);
    });

    setup_sidebar();
    setup_menu();
    setup_login();
    setup_registration();
}
/**********************************************************************************************************************/
function setup_unauth_page() {
    setup_protest_forms();
}
/**********************************************************************************************************************/
$(document).ready(function() {
    setup_unauth_page();
    api.clean_dead_sessions().then(setup_auth_page);
});
/**********************************************************************************************************************/
