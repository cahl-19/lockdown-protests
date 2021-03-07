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
function display(popup, msg) {
    let msg_area = popup.find('.error-message');
    let close_button = popup.find('.close-error-popup');
    let error_overlay = popup.next('.error-overlay');

    msg_area.text(msg);
    error_overlay.css('display', 'block');
    popup.addClass('error-popup-open');
    popup.css('display', 'block');

    return new Promise((accepted) => {
        close_button.on('click.error_popup', () => {
            hide(popup);
            accepted();
        });
    });
}

function hide(popup) {
    let close_button = popup.find('.close-error-popup');
    let error_overlay = popup.next('.error-overlay');

    popup.removeClass('error-popup-open');
    popup.css('display', '');
    error_overlay.css('display', '');
    close_button.off('click.error_popup');
}

export let display_error = {
    'display': display,
    'hide': hide
};

export default display_error;
