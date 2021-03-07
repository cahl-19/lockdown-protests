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
    let msg_area = popup.find('.confirm-message');
    let confirm_button = popup.find('.confirmation-popup-confirm-button');
    let cancel_button = popup.find('.confirmation-popup-cancel-button');
    let overlay = popup.next('.error-overlay');

    msg_area.text(msg);
    overlay.css('display', 'block');
    popup.addClass('confirm-popup-open');
    popup.css('display', 'block');

    return new Promise((accepted, rejected) => {
        confirm_button.on('click.confirm_popup', () => {
            hide(popup);
            accepted();
        });
        cancel_button.on('click.confirm_popup', () => {
            hide(popup);
            rejected();
        });
    });
}

function hide(popup) {
    let confirm_button = popup.find('.confirmation-popup-confirm-button');
    let cancel_button = popup.find('.confirmation-popup-cancel-button');
    let overlay = popup.next('.error-overlay');

    popup.removeClass('confirm-popup-open');
    popup.css('display', '');
    overlay.css('display', '');

    confirm_button.off('click.confirm_popup');
    cancel_button.off('click.confirm_popup');
}

export let confirm = {
    'display': display,
    'hide': hide
};

export default confirm;
