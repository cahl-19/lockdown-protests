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
import jwt_decode from 'jwt-decode';
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const SUCCESS_CODE = 0;
const GENERIC_INTERNAL_ERROR = 1;
const CONTENT_TYPE_ERROR = 2;
const LOGIN_FAILURE = 3;
const UNAUTHORIZED_FAILURE = 4;

const ERROR_CODES = {
    'SUCCESS_CODE': SUCCESS_CODE,
    'GENERIC_INTERNAL_ERROR': GENERIC_INTERNAL_ERROR,
    'CONTENT_TYPE_ERROR': CONTENT_TYPE_ERROR,
    'LOGIN_FAILURE': LOGIN_FAILURE,
    'UNAUTHORIZED_FAILURE': UNAUTHORIZED_FAILURE
};

const CONTENT_TYPE = "application/json; charset=utf-8";

const AUTHORIZATION_STORAGE_KEY = 'authorization';

const LOCAL_STORAGE_AVAILABLE = (() => {
    let test = 'test';
    try {
        localStorage.setItem(test, test);
        if(localStorage.getItem(test) === test) {
            localStorage.removeItem(test);
            return true;
        } else {
            return false;
        }
    } catch(e) {
        return false;
    }
})();
const STORAGE = LOCAL_STORAGE_AVAILABLE ? localStorage : sessionStorage;
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
function is_unauthorized(status, response_body) {
    if(response_body !== undefined) {
        return response_body.code === UNAUTHORIZED_FAILURE;
    } else {
        return status === 401;
    }
}

function parse_error_response(response_text) {
    try{
        return JSON.parse(response_text);
    } catch(e) {
        return undefined;
    }
}

function refresh(success_cb, failure_cb) {

    if(success_cb === undefined) {
        success_cb = () => {};
    }
    if(failure_cb === undefined) {
        failure_cb = () => {};
    }

    let body = {
        'token': STORAGE.getItem(AUTHORIZATION_STORAGE_KEY)
    };

    return $.ajax({
          type: "POST",
          url: "/api/refresh-token",
          data: JSON.stringify(body),
          contentType: CONTENT_TYPE,
          dataType: "json",
          success: function(data){
              STORAGE.setItem("authorization", data.token);
              success_cb();
          },
          error: function(jqXHR, text_status, error_thrown) {
                let data = parse_error_response(jqXHR.responseText);
                if(is_unauthorized(jqXHR.status, data)) {
                    STORAGE.removeItem(AUTHORIZATION_STORAGE_KEY);
                }
                failure_cb(jqXHR.status, data, jqXHR, text_status, error_thrown);
            }
    });
}

function api_call(path, method, data, success_cb, failure_cb) {

        if(success_cb === undefined) {
            success_cb = () => {};
        }
        if(failure_cb === undefined) {
            failure_cb = () => {};
        }

        let token = STORAGE.getItem(AUTHORIZATION_STORAGE_KEY);
        let headers = token ? {"Authorization": `Bearer ${token}`} : {};

        return $.ajax({
              type: method,
              url: path,
              data: method === "GET" ? data : JSON.stringify(data),
              contentType: CONTENT_TYPE,
              dataType: "json",
              headers: headers,
              success: function(data){
                  success_cb(data);
              },
              error: function(jqXHR, text_status, error_thrown) {
                  failure_cb(jqXHR.status, parse_error_response(jqXHR.responseText), jqXHR, text_status, error_thrown);
              }
        });
}

function decode_token() {
    let token = STORAGE.getItem(AUTHORIZATION_STORAGE_KEY);
    return token ? jwt_decode(token) : undefined;
}

export let api = {
    'error_codes': ERROR_CODES,
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
              contentType: CONTENT_TYPE,
              dataType: "json",
              success: function(data){
                  STORAGE.setItem(AUTHORIZATION_STORAGE_KEY, data.token);
                  success_cb();
              },
              error: function(jqXHR, text_status, error_thrown) {
                  failure_cb(jqXHR.status, parse_error_response(jqXHR.responseText), jqXHR, text_status, error_thrown);
              }
        });
    },
    'logout': function(success_cb, failure_cb) {

        if(success_cb === undefined) {
            success_cb = () => {};
        }
        if(failure_cb === undefined) {
            failure_cb = () => {};
        }

        return $.ajax({
              type: "POST",
              url: "/api/logout",
              data: JSON.stringify({}),
              contentType: CONTENT_TYPE,
              dataType: "json",
              success: function(data){
                  STORAGE.removeItem(AUTHORIZATION_STORAGE_KEY);
                  success_cb();
              },
              error: function(jqXHR, text_status, error_thrown) {
                  STORAGE.removeItem(AUTHORIZATION_STORAGE_KEY);
                  failure_cb(jqXHR.status, parse_error_response(jqXHR.responseText), jqXHR, text_status, error_thrown);
              }
        });
    },
    'call': function(path, method, request_body, success_cb, failure_cb) {

        success_cb = success_cb ? success_cb : () => {};
        failure_cb = failure_cb ? failure_cb : () => {};

        api_call(
            path, method, request_body,
            (response_body) => {
                success_cb(response_body);
            },
            (status, error_body, jqXHR, text_status, error_thrown) => {
                if(is_unauthorized(status, error_body)) {
                    if(decode_token() === undefined) {
                        this.logout(
                            () => api_call(path, method, request_body, success_cb, failure_cb),
                            () => failure_cb(status, error_body, jqXHR, text_status, error_thrown)
                        );
                    } else {
                        refresh(
                            () => api_call(path, method, request_body, success_cb, failure_cb),
                            () => failure_cb(status, error_body, jqXHR, text_status, error_thrown)
                        );
                    }
                } else {
                    failure_cb(status, error_body, jqXHR, text_status, error_thrown);
                }
            }
        );
    },
    'whoami': function() {
        let token = STORAGE.getItem(AUTHORIZATION_STORAGE_KEY);
        return token ? jwt_decode(token).username : undefined;
    },
    'current_user_role': function() {
        let token = STORAGE.getItem(AUTHORIZATION_STORAGE_KEY);
        return token ? jwt_decode(token).role : undefined;
    },
    'clean_dead_sessions': function() {

        let token = decode_token();
        let now = Date.now();

        return new Promise( (fufilled) => {

            if(token === undefined) {
                fufilled();
            }

            if(now > (token.exp * 1000)) {
                refresh(fufilled, fufilled);
            } else {
                this.call(
                    '/api/whoami',
                    'GET',
                    {},
                    (data) => {
                        if(!data.apiTokenValid) {
                            STORAGE.removeItem(AUTHORIZATION_STORAGE_KEY);
                        }
                        fufilled();
                    },
                    (status, data) => {
                        if(is_unauthorized(status, data)) {
                            STORAGE.removeItem(AUTHORIZATION_STORAGE_KEY);
                        }
                        fufilled();
                    }
                );
            }
        });
    }
};

export default api;
/**********************************************************************************************************************/