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
package ldprotest.geo.geoip;

import com.mongodb.MongoException;
import com.opencsv.CSVReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import ldprotest.db.MainDatabase;
import ldprotest.db.SetupDatabase;
import ldprotest.geo.geoip.GeoIpLookup.GeoIpTableRow;
import ldprotest.main.AppLogging;
import static ldprotest.main.AppLogging.ResourceType.ON_CLASSPATH;
import ldprotest.util.ErrorCode;
import ldprotest.util.Result;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public final class CmdLineReadIpLocationCsv {

    private static final String LOGBACK_CONFIG = "/config/logback-off.xml";

    private CmdLineReadIpLocationCsv() {
        /* do not construct */
    }

    public static void main(String args[]) {
        Result<Integer, Args> parseResult = parseArgs(args);

        if(parseResult.isFailure()) {
            System.exit(parseResult.failureReason());
        }

        Args options = parseResult.result();

        if(options.helpRequested) {
            System.exit(0);
        }

        try {
            AppLogging.setLogbackConfig(LOGBACK_CONFIG, ON_CLASSPATH);
        }
        catch(IOException e) {
            System.err.println("Error setting logging configuration:" + e);
            System.exit(-1);
        }

        MainDatabase.connect(options.mongoConnect);

        ErrorCode<MongoException> e = MainDatabase.testConnection();

        if(e.failed()) {
            System.err.println("No database connection at startup:" + e);
            System.exit(-1);
        }

        SetupDatabase.setup(MainDatabase.database());

        System.exit(readInput(options));
    }

    private static int readInput(Args options) {
        List<GeoIpTableRow> rows = new ArrayList<>();
        int minRowSize = max(options.highRangeIndex, options.lowRangeIndex, options.latIndex, options.longIndex) + 1;
        int line = 1;

        try (CSVReader reader = new CSVReader(new InputStreamReader(System.in))) {
            String[] columns;
            while ((columns = reader.readNext()) != null) {

                if(columns.length < minRowSize) {
                    System.err.println("Format error at line " + line + " : not enough columns");
                    return -1;
                }

                long lowRangeIp = Long.parseLong(columns[options.lowRangeIndex]);
                long highRangeIp = Long.parseLong(columns[options.highRangeIndex]);
                double latitude = Double.parseDouble(columns[options.latIndex]);
                double longitude = Double.parseDouble(columns[options.longIndex]);

                rows.add(new GeoIpTableRow(lowRangeIp, highRangeIp, latitude, longitude));
                line += 1;
            }
        } catch(IOException ex) {
            System.err.println("IOError encountered while reading CSV.");
            return -1;
        } catch(NumberFormatException ex) {
            System.err.println("Number format error at line " + line);
            return -1;
        }

        try {
            GeoIpLookup.drop();
            GeoIpLookup.write(rows);
        } catch(MongoException ex) {
            System.err.println("Encountered database error while importing file.");
            return -1;
        }

        return 0;
    }

    private static int max(int... args) {

        if(args.length == 0) {
            return Integer.MIN_VALUE;
        }

        int largest = args[0];
        for(int i = 1; i < args.length; i++) {
            largest = Math.max(largest, args[i]);
        }

        return largest;
    }

    private static Result<Integer, Args> parseArgs(String... cmdLine) {
        Args args = new Args();
        int exitCode = new CommandLine(args).execute(cmdLine);

        if(exitCode != 0) {
            return Result.failure(exitCode);
        } else {
            return Result.success(args);
        }
    }

    @Command(
        name = "import-ip-location-csv",
        description = (
            "Reads a csv file containing geo IP data into the database from stdin\n\n" +
            "By default, reads a file formatted as a ip2location lite IP-COUNTRY-REGION-CITY-LATITUDE-LONGITUDE csv " +
            "file. IP address values are formatted as unsigned integer numbers (ex. 127.0.0.1 -> 2130706433). " +
            "Note that proper attribution must be given if using the actual ip2location database.\n\n" +
            "WARNING: This will delete and replace the current contents of the geo ip database collection."
        )
    )
    private static class Args implements Callable<Integer> {
        @CommandLine.Option(
            names={"--ip-lowrange-index"},
            description=(
                "Column index (zero based) of the csv column containing the ip address low range value. " +
                "Default is 0."
            )
        )
        public int lowRangeIndex = 0;

        @CommandLine.Option(
            names={"--ip-highrange-index"},
            description=(
                "Column index (zero based) of the csv column containing the ip address high range value. " +
                "Default is 1."
            )
        )
        public int highRangeIndex = 1;

        @CommandLine.Option(
            names={"--longitude-index"},
            description="Column index (zero based) of the csv column containing the longitude value. Default is 7"
        )
        public int longIndex = 7;

        @CommandLine.Option(
            names={"--latitude-index"},
            description="Column index (zero based) of the csv column containing the latttude value. Default is 6."
        )
        public int latIndex = 6;

        @CommandLine.Option(
            names={"--mongo-connect"},
            description=(
                "Mongo connection string for connecting to DB. Default is to connect to server on localhost with " +
                "username ldprotest and password, ldprotest " +
                "(i.e. mongodb://ldprotest:ldprotest@localhost:27017/?serverSelectionTimeoutMS=3000)"
            )
        )
        public String mongoConnect = "mongodb://ldprotest:ldprotest@localhost:27017/?serverSelectionTimeoutMS=3000";

        @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display help text")
        private boolean helpRequested = false;

        @Override
        public Integer call() throws Exception {
            return 0;
        }
    }
}
