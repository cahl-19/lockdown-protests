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
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const ENTITY_MAP = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#x27;',
  '/': '&#x2F;',
  '`': '&#x60;',
  '=': '&#x3D;'
};

const REV_ENTITY_MAP = {
  '&amp;': '&',
  '&lt;': '<',
  '&gt;': '>',
  '&quot;': '"',
  '&#x27;': "'",
  '&#x2F;': '/',
  '&#x60;': '`',
  '&#x3D;': '='
};
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function __encode_html(text) {
  return String(text).replace(/[&<>"'`=\/]/g, (s) => {
    return ENTITY_MAP[s];
  });
}

function __decode_html(text) {
  return String(text).replace(/&(amp;|lt;|gt;|quot;|#x27;|#x2F;|#x60;|#x3D;)/g, (s) => {
    return REV_ENTITY_MAP[s];
  });
}

export let sanitize = {
    'encode_api_html': function(text) {
        /* Note that the sever is supposed to html encode all user content, so here we first attempt to decode any
         * encoded elements before re-encoding again. If the server did it's job correctly this will do nothing.
         * Otherwise any html unsafe characters will be safely encoded */

        return __encode_html(__decode_html(text));
    },
    'decode_api_html': function(text) {
        /* Note that the sever is supposed to html encode all user content, so here we first attempt to decode any
         * encoded elements before re-encoding again. If the server did it's job correctly this will do nothing.
         * Otherwise any html unsafe characters will be safely encoded */

        return __decode_html(text);
    },
    'jquery_set_elem_text': function(elem_text) {
        /**
         * Should be preferred to calling encode_api_html. Jquery's text method will safely html encode a string. The
         * server, however, will (should) also always send html encoded data. So, we must attempt a decode of the data
         * before passing it to jquery text method in order to avoid double encoding.
         */
        return elem.text(__decode_html(elem_text));
    }
};

export default sanitize;
