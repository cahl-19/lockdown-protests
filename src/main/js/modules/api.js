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
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const SUCCESS_CODE = 0;
const GENERIC_INTERNAL_ERROR = 1;
const CONTENT_TYPE_ERROR = 2;
const LOGIN_FAILURE = 3;
const UNAUTHORIZED_FAILURE = 4;

const AUTHORIZATION_STORAGE_KEY = 'authorization';
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function refresh(success_cb, failure_cb) {
        let refresh = {
            'token': localStorage.getItem(AUTHORIZATION_STORAGE_KEY)
        };

        return $.ajax({
              type: "POST",
              url: "/api/refresh-token",
              data: JSON.stringify(refresh),
              contentType: "application/json; charset=utf-8",
              dataType: "json",
              success: function(data){
                  localStorage.setItem("authorization", data.token);
                  success_cb();
              },
              error: function(jqXHR, text_status, error_thrown) {
                  failure_cb(jqXHR.status, JSON.parse(jqXHR.responseText), jqXHR, text_status, error_thrown);
              }
        });
}
/**********************************************************************************************************************/
function api_call(path, method, data, success_cb, failure_cb) {

        if(success_cb === undefined) {
            success_cb = () => {};
        }
        if(failure_cb === undefined) {
            failure_cb = () => {};
        }

        return $.ajax({
              type: method,
              url: path,
              data: JSON.stringify(data),
              contentType: "application/json; charset=utf-8",
              dataType: "json",
              headers: {
                "Authorization": `Bearer ${localStorage.getItem(AUTHORIZATION_STORAGE_KEY)}`
              },
              success: function(data){
                  success_cb(data);
              },
              error: function(jqXHR, text_status, error_thrown) {
                  failure(jqXHR.status, JSON.parse(jqXHR.responseText), jqXHR, text_status, error_thrown);
              }
        });
}
/**********************************************************************************************************************/
export let api = {
    'login': function(username, password, success_cb, failure_cb) {

        let credentials = {
            'username': username,
            'password': password
        };

        if(success_cb === undefined) {
            success_cb = () => {};
        }
        if(failure_cb === undefined) {
            failure_cb = () => {};
        }

        return $.ajax({
              type: "POST",
              url: "/api/login",
              data: JSON.stringify(credentials),
              contentType: "application/json; charset=utf-8",
              dataType: "json",
              success: function(data){
                  localStorage.setItem(AUTHORIZATION_STORAGE_KEY, data.token);
                  success_cb();
              },
              error: function(jqXHR, text_status, error_thrown) {
                  failure_cb(jqXHR.status, JSON.parse(jqXHR.responseText), jqXHR, text_status, error_thrown);
              }
        });
    },
    'call': function(path, method, data, success_cb, failure_cb) {
        if(success_cb === undefined) {
            success_cb = () => {};
        }
        if(failure_cb === undefined) {
            failure_cb = () => {};
        }

        api_call(
                path, method, data,
                (data) => {
                    success_cb(data);
                },
                (status, data, jqXHR, text_status, error_thrown) => {
                    if(data.code === UNAUTHORIZED_FAILURE) {
                        refresh(
                                api_call(path, method, data, success_cb, failure_cb), failure_cb
                        );
                    }
                }
        );
    },
};
/**********************************************************************************************************************/
export default api;
/**********************************************************************************************************************/