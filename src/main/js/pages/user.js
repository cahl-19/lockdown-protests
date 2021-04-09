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
import '!style-loader!css-loader!bootstrap/dist/css/bootstrap.min.css';

import Popper from 'popper.js';
import 'bootstrap';

import display_error from 'display-error';
import $ from 'jquery';
import api from 'api';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function popup_error(message) {
    let error_popup = $('#error-popup');
    return display_error.display(error_popup, message);
}

function subject_user() {
    let params = new URLSearchParams(window.location.search);
    return params.get('uname');
}

function init_title() {
    let uname = subject_user();

    if(!uname) {
        popup_error('Error: parameter "uname" undefined');
    } else {
        $('#user-title').text(uname);
    }
}

function init_info() {
    api.call(
        `/api/user/${subject_user()}`,
        'GET',
        {},
        (user_info) => {
            $('#user-info-username').text(user_info.username);
            $('#user-info-email').text(user_info.email);
            $('#user-info-role').text(user_info.role);
        },
        (status) => {
            if(status === 401) {
                popup_error('Unauthorized.');
            } else if(status === 404) {
                popup_error('User not found.');
            } else {
                popup_error('An error occured.');
            }
        }
    );
}

$(document).ready(function() {
    init_title();
    init_info();
});
