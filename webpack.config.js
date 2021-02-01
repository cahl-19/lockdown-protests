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
*                                                       REQUIRES                                                       *
***********************************************************************************************************************/
const fs = require('fs');
const path = require('path');
const process = require('process');
/***********************************************************************************************************************
*                                                      CONSTANTS                                                       *
***********************************************************************************************************************/
const JS_SOURCE_ROOT = 'src/main/js/pages';
const JS_MODULES_ROOT = 'src/main/js/modules';
const NODE_MODULES_PATH = 'build/node_modules';
const CACHE = path.join(NODE_MODULES_PATH, ".cache");
const OUTPUT_DIRECTORY = 'build/webpack/webpack-bundles';
/***********************************************************************************************************************
*                                                   PLUGIN REQUIRES                                                    *
***********************************************************************************************************************/
process.env.NODE_PATH =  path.resolve(__dirname, NODE_MODULES_PATH);
require("module").Module._initPaths();

const TerserPlugin = require('terser-webpack-plugin');
/***********************************************************************************************************************
*                                                      FUNCTIONS                                                       *
***********************************************************************************************************************/
function recursive_list_files(dir, pattern) {

    var results = [];
    var list = fs.readdirSync(dir);

    if(pattern === undefined) {
        pattern = /.*/;
    }

    list.forEach(function(file) {
        file = path.join(dir, file);
        var stat = fs.statSync(file);

        if (stat && stat.isDirectory()) {
            results = results.concat(recursive_list_files(file, pattern));
        } else if (pattern.test(file)){
            results.push(path.resolve(__dirname, file));
        }
    });

    return results;
}
/***********************************************************************************************************************
*                                                        CONFIG                                                        *
***********************************************************************************************************************/
module.exports = {
    "context": path.join(__dirname, "./"),
    "entry": recursive_list_files(JS_SOURCE_ROOT, /.*\.js/).reduce((map, file) => {
        let b  = path.basename(file, ".js");
        map[b] = file;
        return map;
    }, {}),
    "resolve": {
        "modules": [path.resolve(__dirname, NODE_MODULES_PATH), path.resolve(__dirname, JS_MODULES_ROOT)]
    },
    "resolveLoader": {
        "modules": [path.resolve(__dirname, NODE_MODULES_PATH), path.resolve(__dirname, JS_MODULES_ROOT)]
    },
    "cache": {
        "type": "filesystem",
        "cacheDirectory": path.resolve(__dirname, CACHE)
    },
    optimization: {
        "minimize": true,
        "minimizer": [new TerserPlugin({cache: path.resolve(__dirname, CACHE)})],
        "splitChunks": {
            "chunks": "all"
        }
    },
    output: {
        "path": path.resolve(__dirname, OUTPUT_DIRECTORY)
    }
};
/**********************************************************************************************************************/
