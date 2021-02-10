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

import Popper from 'popper.js';
import 'bootstrap';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function setup_echo_test() {
    var textInput = $("#echo-test > input");

    $("#echo-test > button").click((ev) => {
       ev.preventDefault();

        $.ajax({
            type: "POST",
            url: "/api/test/echo",
            data: JSON.stringify({ "message": textInput.val() }),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function(data){alert(data.message);},
            error: function(errMsg) {
                alert(errMsg);
            }
        });
    });
}
/**********************************************************************************************************************/
$(document).ready(function() {
   setup_echo_test();
});
/**********************************************************************************************************************/
