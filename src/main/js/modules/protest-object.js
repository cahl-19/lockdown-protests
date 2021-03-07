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
import sanitize from 'sanitize';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/

function zero_pad(num, length) {
    let s = `${num}`;

    while(s.length < length) {
        s = '0' + s;
    }

    return s;
}

export let protest_obj = {
    'Protest': class Protest {
        constructor(fields) {
            this.title = fields.title ? sanitize.encode_api_html(fields.title) : fields.title;
            this.owner = fields.owner ? sanitize.encode_api_html(fields.owner) : fields.owner;
            this.description = fields.description ? sanitize.encode_api_html(fields.description) : fields.description;
            this.dressCode = fields.dressCode ? sanitize.encode_api_html(fields.dressCode) : fields.dressCode;
            this.date = fields.date ? new Date(fields.date) : undefined;
            this.location = fields.location;
            this.protestId = fields.protestId;
            this.homePage = fields.homePage;
        }
        htmlDecodedTitle() {
           return this.title ? sanitize.decode_api_html(this.title) : this.title;
        }
        htmlDecodedOwner() {
            return this.owner ? sanitize.decode_api_html(this.owner) : this.owner;
        }
        htmlDecodedDescription() {
            return this.description ? sanitize.decode_api_html(this.description) : this.description;
        }
        htmlDecodedDressCode() {
            return this.dressCode ? sanitize.decode_api_html(this.dressCode) : this.dressCode;
        }
        plainEncodedDocument() {
            return {
                'title': this.title,
                'owner': this.owner,
                'description': this.description,
                'dressCode': this.dressCode,
                'date': this.date.getTime(),
                'location': this.location,
                'protestId': this.protestId,
                'homePage': this.homePage
            }
        }
        plainUnencodedDocument() {
            return {
                'title': this.htmlDecodedTitle(),
                'owner': this.htmlDecodedOwner(),
                'description': this.htmlDecodedDescription(),
                'dressCode': this.htmlDecodedDressCode(),
                'date': this.date.getTime(),
                'location': this.location,
                'protestId': this.protestId,
                'homePage': this.homePage
            }
        }
        date_input_val() {
            let date = this.date;

            if(date === undefined) {
                return undefined;
            }

            return `${date.getFullYear()}-${zero_pad(date.getMonth() + 1, 2)}-${zero_pad(date.getDate(), 2)}`;
        }
        time_input_val() {
            let date = this.date;

            if(date === undefined) {
                return undefined;
            }

            return `${zero_pad(date.getHours(), 2)}:${zero_pad(date.getMinutes(), 2)}`;
        }
    }
};

export default protest_obj;
