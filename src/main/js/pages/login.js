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
import api from 'api';

import '!style-loader!css-loader!bootstrap/dist/css/bootstrap.min.css';

import Popper from 'popper.js';
import 'bootstrap';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function setup_login_form() {

    $('#submit-login').on('click', (ev) => {
        ev.preventDefault();

        api.login(
            $('#email-input').val(), $('#password-input').val(),
            () => {
                window.location.replace('/');
            },
            (status, error) => {
                alert(`Error: ${status} - ${error.description}`);
            }
        );
    });
}

$(document).ready(function() {
   api.clean_dead_sessions().then(setup_login_form);
});
