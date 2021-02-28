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
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
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
export let protest_form = {
    'on_hide': function(id_prefix, fun) {
        return $(`#${id_prefix}-form-modal`).on('hidden.bs.modal', fun);
    },
    'open_form': function(id_prefix, lat, lng) {
        let modal = $(`#${id_prefix}-form-modal`);

        $(`#${id_prefix}-input-latitude`).val(lat);
        $(`#${id_prefix}-input-longitude`).val(lng);

        modal.modal('show');
    },
    'setup_form': function(id_prefix, display_error) {

        let form = $(`#${id_prefix}-form`);
        let submit_button = $(`#${id_prefix}-form-submit`);

        let title = $(`#${id_prefix}-input-title`);
        let dress_code = $(`#${id_prefix}-input-dress-code`);
        let description = $(`#${id_prefix}-input-description`);
        let date = $(`#${id_prefix}-input-date`);
        let time = $(`#${id_prefix}-input-time`);
        let dt_validity_feedback = $(`#${id_prefix}-date-time-validity-feedback`);


        title.attr('maxlength', 256);
        title.attr('minlength', 4);

        description.attr('maxlength', 768);
        description.attr('minlength', 8);

        dress_code.attr('maxlength', 128);

        window.setInterval(() => {
            let dt = new Intl.DateTimeFormat([], { dateStyle: 'full', timeStyle: 'long' }).format(Date.now());
            $(`${id_prefix}-user-current-time`).text(dt);
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
                    'latitude': $(`#${id_prefix}-input-latitude`).val(),
                    'longitude': $(`#${id_prefix}-input-longitude`).val()
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
                (status, error) => display_error(status, error)
            );
        });
    }
};
/**********************************************************************************************************************/
export default protest_form;
/**********************************************************************************************************************/