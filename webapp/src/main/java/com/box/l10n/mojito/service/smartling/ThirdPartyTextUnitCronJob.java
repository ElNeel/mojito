package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.slack.SlackClientException;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.stereotype.Component;

/**
 * @author cegbukichi
 */
@Profile("!disablescheduling")
@ConditionalOnProperty(value="l10n.thirdPartyTextUnit.type")
@Configuration
@Component
@DisallowConcurrentExecution
public class ThirdPartyTextUnitCronJob implements Job {

    static Logger logger = LoggerFactory.getLogger(ThirdPartyTextUnitCronJob.class);

    @Value("${l10n.thirdPartyTextUnit.cron}")
    String thirdPartyTextUnitCron;

    @Autowired
    TaskScheduler taskScheduler;

    @Autowired
    ThirdPartyTextUnitMatchingService thirdPartyTextUnitMatchingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Starting thirdPartyTextUnitCronJob execution");
        Integer assetId = 1;
        try {
            thirdPartyTextUnitMatchingService.getThirdPartyTextUnits("1234", "demo.properties", assetId.longValue());
        } catch (SmartlingClientException e) {
            logger.info("Exception pulling third party source strings");
        }
    }

    @Bean(name = "thirdPartyTextUnitCron")
    public JobDetailFactoryBean jobDetailThirdPartyTextUnitCronJob() {
       JobDetailFactoryBean jobDetailFactoryBean = new JobDetailFactoryBean();
       jobDetailFactoryBean.setJobClass(ThirdPartyTextUnitCronJob.class);
       jobDetailFactoryBean.setDescription("Check for third party text units and match with mojito text units");
       jobDetailFactoryBean.setDurability(true);
       return jobDetailFactoryBean;
    }

    @Bean
    public CronTriggerFactoryBean triggerThirdPartyTextUnitCronJob(@Qualifier("thirdPartyTextUnitCron") JobDetail job) {
        logger.info("Triggering thirdPartyTextUnitCronJob");
        CronTriggerFactoryBean trigger = new CronTriggerFactoryBean();
        trigger.setJobDetail(job);
        trigger.setCronExpression(thirdPartyTextUnitCron);
        return trigger;
    }

}