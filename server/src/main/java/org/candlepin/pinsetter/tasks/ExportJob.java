package org.candlepin.pinsetter.tasks;

import static org.quartz.JobBuilder.newJob;

import org.candlepin.common.exceptions.ForbiddenException;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.sync.ExportCreationException;
import org.candlepin.sync.ExportResult;
import org.candlepin.sync.Exporter;
import org.candlepin.util.Util;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import com.google.inject.Inject;

public class ExportJob extends UniqueByEntityJob {

    protected static final String CDN_LABEL = "cdn_label";
    protected static final String WEBAPP_PREFIX = "webapp_prefix";
    protected static final String API_URL = "api_url";

    private static Logger log = LoggerFactory.getLogger(ExportJob.class);

    private Exporter exporter;
    private ConsumerCurator consumerCurator;
    private I18n i18n;

    @Inject
    public ExportJob(Exporter exporter, ConsumerCurator consumerCurator, I18n i18n) {
        this.exporter = exporter;
        this.consumerCurator = consumerCurator;
        this.i18n = i18n;
    }

    @Override
    public void toExecute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap map = context.getMergedJobDataMap();
        String consumerUuid = map.getString(JobStatus.TARGET_ID);
        String cdnLabel = map.getString(CDN_LABEL);
        String webAppPrefix = map.getString(WEBAPP_PREFIX);
        String apiUrl = map.getString(API_URL);

        Consumer consumer = validateConsumer(consumerUuid);

        log.info("Starting async export for {}", consumerUuid);
        try {
            ExportResult result = exporter.generateAndStoreExport(consumer, cdnLabel, webAppPrefix, apiUrl);
            context.setResult(result);
        }
        catch (ExportCreationException e) {
            // TODO Delete the generated archive on Failure.
            throw new JobExecutionException(e.getMessage(), e, false);
        }
    }

    private Consumer validateConsumer(String consumerUuid) {
        Consumer consumer = consumerCurator.verifyAndLookupConsumer(consumerUuid);
        if (consumer.getType() == null ||
            !consumer.getType().isManifest()) {
            throw new ForbiddenException(
                i18n.tr(
                    "Unit {0} cannot be exported. " +
                    "A manifest cannot be made for units of type ''{1}''.",
                    consumerUuid, consumer.getType().getLabel()));
        }
        return consumer;
    }

    public static JobDetail scheduleExport(Consumer consumer, String cdnLabel, String webAppPrefix,
        String apiUrl) {
        JobDataMap map = new JobDataMap();
        map.put(JobStatus.OWNER_ID, consumer.getOwner().getKey());
        map.put(JobStatus.TARGET_TYPE, JobStatus.TargetType.CONSUMER);
        map.put(JobStatus.TARGET_ID, consumer.getUuid());
        map.put(CDN_LABEL, cdnLabel);
        map.put(WEBAPP_PREFIX, webAppPrefix);
        map.put(API_URL, apiUrl);

        return newJob(ExportJob.class)
            .withIdentity("export_" + Util.generateUUID())
            .usingJobData(map)
            .build();
    }

}
