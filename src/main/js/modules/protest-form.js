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
import protest_object from 'protest-object';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function validate_url(url) {
    return /^(www|http|https):\/\/[^ "]+$/.test(url);
}

function input_value(inp) {
    let val = inp.val();

    if(val === undefined || val === '') {
        return undefined;
    } else {
        return val;
    }
}

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
        set_if_defined($(`#${id_prefix}-input-title`), populate.htmlDecodedTitle());
        set_if_defined($(`#${id_prefix}-input-dress-code`), populate.htmlDecodedDressCode);
        set_if_defined($(`#${id_prefix}-input-date`), populate.date_input_val());
        set_if_defined($(`#${id_prefix}-input-time`), populate.time_input_val());
        set_if_defined($(`#${id_prefix}-input-protest-id`), populate.protestId);
        set_if_defined($(`#${id_prefix}-input-description`), populate.htmlDecodedDescription());
        set_if_defined($(`#${id_prefix}-input-home-page`), populate.homePage);

        $(`#${id_prefix}-display-location`).text(
            `Lat: ${populate.location.latitude.toFixed(3)}, ` +
            `Long: ${populate.location.longitude.toFixed(3)}`
        );

        modal.modal('show');
    },
    'setup_form': function(id_prefix, submit, delete_protest) {

        let form = $(`#${id_prefix}-form`);
        let submit_button = $(`#${id_prefix}-form-submit`);
        let delete_button = $(`#${id_prefix}-protest-delete`);

        let title = $(`#${id_prefix}-input-title`);
        let dress_code = $(`#${id_prefix}-input-dress-code`);
        let description = $(`#${id_prefix}-input-description`);
        let date = $(`#${id_prefix}-input-date`);
        let time = $(`#${id_prefix}-input-time`);
        let dt_validity_feedback = $(`#${id_prefix}-date-time-validity-feedback`);
        let protest_id = $(`#${id_prefix}-input-protest-id`);
        let user_current_time = $(`#${id_prefix}-user-current-time`);
        let home_page = $(`#${id_prefix}-input-home-page`);
        let latitude = $(`#${id_prefix}-input-latitude`);
        let longitude = $(`#${id_prefix}-input-longitude`);

        title.attr('maxlength', 256);
        title.attr('minlength', 4);

        description.attr('maxlength', 768);
        description.attr('minlength', 8);

        dress_code.attr('maxlength', 128);

        window.setInterval(() => {
            let dt = new Intl.DateTimeFormat([], {
                'year': 'numeric',
                'month': 'numeric',
                'day': 'numeric',
                'hour': 'numeric',
                'minute': 'numeric',
                'second': 'numeric'
            }).format(Date.now());
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
                    'latitude': input_value(latitude),
                    'longitude': input_value(longitude)
                },
                'owner': api.whoami(),
                'title': input_value(title),
                'description': input_value(description),
                'dressCode': input_value(dress_code),
                'date': date_from_inputs(date, time).getTime(),
                'protestId': input_value(protest_id),
                'homePage': input_value(home_page)
            };
        }

        date.on('input', validate_date);
        time.on('input', validate_date);

        submit_button.on('click', () => form.submit());

        form.submit((ev) => {

            let home_page_val = home_page.val().trim();

            ev.preventDefault();
            ev.stopPropagation();

            validate_date();

            if(home_page_val && !validate_url(home_page_val)) {
                home_page[0].setCustomValidity('Invalid URL');
            }

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

        delete_button.on('click', () => delete_protest(new protest_object.Protest(protest_data()), delete_button));
    }
};

export default protest_form;
