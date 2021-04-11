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
*                                                         CODE                                                         *
***********************************************************************************************************************/
function strict_parse_int(str) {
    if(!str || /[^0-9]/.test(str)) {
        return undefined;
    }
    return parseInt(str);
}

function strict_parse_float(str) {
    if(!str || !/^-?[0-9]+(\.[0-9]+)?$/.test(str)) {
        return undefined;
    }
    return parseFloat(str);
}

function default_if_undefined(val, def) {
    if(val === undefined) {
        return def;
    } else {
        return val;
    }
}

export let util = {
    'strict_parse_int': strict_parse_int,
    'strict_parse_float': strict_parse_float,
    'default_if_undefined': default_if_undefined
};

export default util;
