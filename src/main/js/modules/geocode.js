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
import protest_map from 'protest-map';
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const SEARCH_STATE = {
    'INITIAL': 0,
    'LOADING': 1,
    'DISPLAYING': 2
};

const DEFAULT_ICON_SRC = '/assets/bootstrap/icons/search.svg';
const GEOCODE_BASE_URL = 'https://api.mapbox.com/geocoding/v5/mapbox.places';
const DEFAULT_LOCATION_ZOOM = 15;
/***********************************************************************************************************************
*                                                         CODE                                                         *
***********************************************************************************************************************/
class search {

    constructor(container, map, config={}) {
        this.container = container;
        this.map = map;
        this.config = config;

        this.search_box = search.__create_search_box(container.icon_src);
        this.api_token = config.token;

        this.dynamic = {
            'state': SEARCH_STATE.INITIAL,
            'query_xhr': undefined
        };
    }

    start() {
        this.container.append(this.search_box.box);

        this.search_box.input.keyup((ev) => {
            if(this.dynamic.state === SEARCH_STATE.LOADING) {
                return;
            }
            if (this.search_box.input.is(":focus") && ev.key === "Enter") {
                this.query(this.search_box.input.val());
            }
        });

        this.search_box.icon.on('click', () => {
            if(this.dynamic.state !== SEARCH_STATE.LOADING) {
                this.query(this.search_box.input.val());
            }

        });
    }

    query(search_string) {

        if(!search_string) {
            return;
        }

        this.__close_results();

        let center = this.map.getCenter();
        let lng_center = protest_map.normalize_longitude(center.lng);
        let lat_center = center.lat;

        let url = (
            `${GEOCODE_BASE_URL}/${encodeURIComponent(search_string)}.json?` +
            `access_token=${this.api_token}&proximity=${lng_center},${lat_center}`
        );

        this.dynamic.state = SEARCH_STATE.LOADING;
        this.__display_spinner();

        return new Promise((done, error) => {
            this.dynamic.query_xhr = $.ajax({
                'type': 'GET',
                'url': url,
                'dataType': 'json',
                'success': (data) => {
                    this.__display_results(this.__parse_response(data));
                    done(data);
                },
                'error': (jqXHR, text_status, error_thrown) => {
                    this.__close_results();
                    this.config.display_error('Unable to perform search');
                    error(jqXHR, text_status, error_thrown);
                }
            });
        });
    }

    change_token(token) {
        this.api_token = token;
    }

    __open_results() {
        let results_container = $('<div class="search-results-container">');
        let close_button = $(
                '<button class="close" aria-label="Close"><span aria-hidden="true">&times;</span></button>'
        );
        let header = $('<div class="search-results-header"><strong>Search Results:</strong></div>');

        close_button.on('click', () => this.__close_results());

        header.append(close_button);
        results_container.append(header);

        this.search_box.box.append(results_container);
        return results_container;
    }

    __close_results() {
        if(this.dynamic.state === SEARCH_STATE.LOADING && this.dynamic.query_xhr !== undefined) {
            this.dynamic.query_xhr.abort();
        }

        this.search_box.box.find('.search-results-container').remove();
        this.dynamic.state = SEARCH_STATE.INITIAL;
    }

    __display_spinner() {
        let results_container = this.__open_results();
        let spinner = $(
            '<div class="spinner-border search-spinner" role="status"><span class="sr-only">Loading...</span></div>'
        );
        let spinner_background = $('<div class="search-spinner-background">');

        spinner_background.append(spinner);
        results_container.append(spinner_background);
    }

    __stop_spinner() {
        this.search_box.box.find('.search-spinner-background').remove();
    }

    __parse_response(results) {
        return results.features.map((f) => {
            let ret =  {
              'place_name': f.place_name,
              'center': f.center,
              'relevance': f.relevance
            };
            if(f.bbox !== undefined) {
               ret.bbox = [
                   [f.bbox[0], f.bbox[1]],
                   [f.bbox[2], f.bbox[3]]
               ];
            }
            return ret;
        });
    }

    __display_results(results) {

        if(this.dynamic.state === SEARCH_STATE.LOADING) {
            this.__stop_spinner();
        } else {
            this.__close_results();
            this.__open_results()
        }

        let results_container = this.search_box.box.find('.search-results-container');
        this.dynamic.state = SEARCH_STATE.DISPLAYING;

        if(results.length === 0) {
            let empty_result = $('<div class="empty-search-result">');
            empty_result.text('No results found. Please refine your query and try again.');
            results_container.append(empty_result);
        } else {
            for(let r of results) {
                let result_display = $('<div class="search-result">');
                result_display.text(r.place_name);

                result_display.on('click', () => {

                    let zoom = r.bbox ? this.map.getBoundsZoom(r.bbox) : DEFAULT_LOCATION_ZOOM;
                    this.__close_results();
                    this.map.flyTo([r.center[1], r.center[0]], zoom);
                });

                results_container.append(result_display);
            }
        }
    }

    static __create_search_box(icon_src=DEFAULT_ICON_SRC) {
        let search_container = $('<div class="search-box">');
        let search_input = $('<input type="text" class="search-input">');
        let search_icon = $(`<img src="${icon_src}" alt="search" class="search-icon">`);

        search_container.append(search_input);
        search_container.append(search_icon);

        return {
            'box': search_container,
            'input': search_input,
            'icon': search_icon
        };
    }
};

export let geocode = {
    'search': (container, map, config) => new search(container, map, config)
};

export default geocode;