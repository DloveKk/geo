/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;

public final class RulesDao {

    private static final Logger LOGGER = Logging.getLogger(RulesDao.class);
    private static final SecureXStream xStream;

    static {
        xStream = new SecureXStream();
        xStream.registerConverter(new RuleConverter());
        xStream.alias("Rule", Rule.class);
        xStream.alias("Rules", RuleList.class);
        xStream.addImplicitCollection(RuleList.class, "rules");
        xStream.allowTypes(new Class[] {Rule.class, RuleList.class});
    }

    public static String getRulesPath() {
        return "params-extractor/extraction-rules.xml";
    }

    public static String getTempRulesPath() {
        return String.format("params-extractor/%s-extraction-rules.xml", UUID.randomUUID());
    }

    public static List<Rule> getRules() {
        Resource rules = getDataDirectory().get(getRulesPath());
        return getRules(() -> rules.in());
    }

    private static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    public static List<Rule> getRules(Supplier<InputStream> iss) {
        try (InputStream inputStream = iss.get()) {
            if (inputStream.available() == 0) {
                Utils.info(LOGGER, "Parameters extractor rules file seems to be empty.");
                return new ArrayList<>();
            }

            RuleList list = (RuleList) xStream.fromXML(inputStream);
            List<Rule> rules = list.rules == null ? new ArrayList<>() : list.rules;
            Utils.info(LOGGER, "Parameters extractor loaded %d rules.", rules.size());
            return rules;
        } catch (Exception exception) {
            throw Utils.exception(exception, "Error parsing rules files.");
        }
    }

    public static void saveOrUpdateRule(Rule rule) {
        Resource rules = getDataDirectory().get(getRulesPath());
        Resource tmpRules = getDataDirectory().get(getTempRulesPath());
        saveOrUpdateRule(rule, () -> rules.in(), () -> tmpRules.out());
        rules.delete();
        tmpRules.renameTo(rules);
    }

    public static void saveOrUpdateRule(
            Rule rule, Supplier<InputStream> iss, Supplier<OutputStream> oss) {
        List<Rule> rules = getRules(iss);
        boolean exists = false;
        for (int i = 0; i < rules.size() && !exists; i++) {
            if (rules.get(i).getId().equals(rule.getId())) {
                rules.set(i, rule);
                exists = true;
            }
        }
        if (!exists) {
            rules.add(rule);
        }
        writeRules(rules, oss);
        Utils.info(LOGGER, "Parameters extractor rules updated.");
    }

    public static void deleteRules(String... rulesIds) {
        Resource rules = getDataDirectory().get(getRulesPath());
        Resource tmpRules = getDataDirectory().get(getTempRulesPath());
        deleteRules(() -> rules.in(), () -> tmpRules.out(), rulesIds);
        rules.delete();
        tmpRules.renameTo(rules);
    }

    public static void deleteRules(
            Supplier<InputStream> inputStream,
            Supplier<OutputStream> outputStream,
            String... ruleIds) {
        List<Rule> rules =
                getRules(inputStream).stream()
                        .filter(rule -> !matchesAnyRuleId(rule, ruleIds))
                        .collect(Collectors.toList());
        writeRules(rules, outputStream);
        Utils.info(LOGGER, "Deleted one or more parameters extractor rules.");
    }

    private static boolean matchesAnyRuleId(Rule rule, String[] ruleIds) {
        return Arrays.stream(ruleIds).anyMatch(ruleId -> ruleId.equals(rule.getId()));
    }

    private static void writeRules(List<Rule> rules, Supplier<OutputStream> oss) {
        try (OutputStream outputStream = oss.get()) {
            xStream.toXML(new RuleList(rules), outputStream);
        } catch (Exception exception) {
            throw Utils.exception(exception, "Something bad happen when writing rules.");
        }
    }

    /** Support class for XStream serialization */
    static final class RuleList {
        List<Rule> rules;

        public RuleList(List<Rule> rules) {
            this.rules = rules;
        }
    }
}
