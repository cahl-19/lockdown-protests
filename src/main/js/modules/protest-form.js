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
import spinner from 'spinner';
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
function zero_pad(num, length) {
    let s = `${num}`;

    while(s.length < length) {
        s = '0' + s;
    }

    return s;
}
/**********************************************************************************************************************/
function date_to_date_input(date) {
    if(date === undefined) {
        return undefined;
    }

    return `${date.getFullYear()}-${zero_pad(date.getMonth() + 1, 2)}-${zero_pad(date.getDay(), 2)}`;
}
/**********************************************************************************************************************/
function date_to_time_input(date) {

    if(date === undefined) {
        return undefined;
    }

    return `${zero_pad(date.getHours(), 2)}:${zero_pad(date.getMinutes(), 2)}`;
}
/**********************************************************************************************************************/
export let protest_form = {
    'on_hide': function(id_prefix, fun) {
        return $(`#${id_prefix}-form-modal`).on('hidden.bs.modal', fun);
    },
    'close_form': function(id_prefix) {
        $(`#${id_prefix}-form-modal`).modal('hide');
    },
    'open_form': function(id_prefix, populate) {
        let modal = $(`#${id_prefix}-form-modal`);

        function set_if_defined(elem, value) {
            if(value !== undefined && value !== '') {
                elem.val(value);
            }
        }

        set_if_defined($(`#${id_prefix}-input-latitude`), populate.location.latitude);
        set_if_defined($(`#${id_prefix}-input-longitude`), populate.location.longitude);
        set_if_defined($(`#${id_prefix}-input-title`), populate.title);
        set_if_defined($(`#${id_prefix}-input-dress-code`), populate.dressCode);
        set_if_defined($(`#${id_prefix}-input-date`), date_to_date_input(populate.date));
        set_if_defined($(`#${id_prefix}-input-time`), date_to_time_input(populate.date));
        set_if_defined($(`#${id_prefix}-input-protest-id`), populate.protestId);
        set_if_defined($(`#${id_prefix}-input-description`), populate.description);

        console.log(populate.protestId);

        $(`#${id_prefix}-display-location`).text(
            `Lat: ${populate.location.latitude.toFixed(3)}, ` +
            `Long: ${populate.location.longitude.toFixed(3)}`
        );

        modal.modal('show');
    },
    'setup_form': function(id_prefix, submit, delete_protest) {

        let form = $(`#${id_prefix}-form`);
        let submit_button = $(`#${id_prefix}-form-submit`);
        let delete_button = $(`#${id_prefix}--protest-delete`);

        let title = $(`#${id_prefix}-input-title`);
        let dress_code = $(`#${id_prefix}-input-dress-code`);
        let description = $(`#${id_prefix}-input-description`);
        let date = $(`#${id_prefix}-input-date`);
        let time = $(`#${id_prefix}-input-time`);
        let dt_validity_feedback = $(`#${id_prefix}-date-time-validity-feedback`);
        let protest_id = $(`#${id_prefix}-input-protest-id`);
        let user_current_time = $(`#${id_prefix}-user-current-time`);


        title.attr('maxlength', 256);
        title.attr('minlength', 4);

        description.attr('maxlength', 768);
        description.attr('minlength', 8);

        dress_code.attr('maxlength', 128);

        window.setInterval(() => {
            let dt = new Intl.DateTimeFormat([], { dateStyle: 'full', timeStyle: 'long' }).format(Date.now());
            user_current_time.text(dt);
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

        function protest_data() {
            return {
                'location': {
                    'latitude': $(`#${id_prefix}-input-latitude`).val(),
                    'longitude': $(`#${id_prefix}-input-longitude`).val()
                },
                'owner': api.whoami(),
                'title': title.val(),
                'description': description.val(),
                'dressCode': dress_code.val(),
                'date': date_from_inputs(date, time).getTime(),
                'protestId': protest_id.val()
            };
        }

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

            if(api.whoami() === undefined) {
                return;
            }

            submit(protest_data(), submit_button);
        });

        delete_button.on('click', () => delete_protest(protest_data()));
    }
};
/**********************************************************************************************************************/
export default protest_form;
/**********************************************************************************************************************/