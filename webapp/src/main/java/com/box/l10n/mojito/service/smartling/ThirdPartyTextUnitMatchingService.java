package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.smartling.SmartlingClient;
import com.box.l10n.mojito.smartling.SmartlingClientException;
import com.box.l10n.mojito.smartling.request.StringData;
import com.box.l10n.mojito.smartling.response.SourceStringsResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service to manage matching between mojito text units and third party text units
 **/
@Component
public class ThirdPartyTextUnitMatchingService {
    /**
     * logger
     */
    static Logger logger = getLogger(ThirdPartyTextUnitMatchingService.class);

    @Autowired
    ThirdPartyTextUnitRepository thirdPartyTextUnitRepository;

    @Autowired
    SmartlingClient smartlingClient;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    void getThirdPartyTextUnits(String projectId, String file, Long assetId) throws SmartlingClientException {
        logger.debug("Checking for third party text units in file {} of project {}", file, projectId);
        Integer offset = 0;
        List<StringData> stringDataToCheck = new ArrayList<>();
        try {
            SourceStringsResponse sourceStrings = getSourceStrings(projectId, file, offset);
            if (sourceStrings != null) {
                stringDataToCheck.add(sourceStrings.getResponse().getData());
                while (sourceStrings.getResponse().getData().getTotalCount() >= 500) {
                    sourceStrings = getSourceStrings(projectId, file, offset);
                    if (sourceStrings.isSuccessResponse()) {
                        stringDataToCheck.add(sourceStrings.getResponse().getData());
                        offset += 500;
                    }
                }
            }
        } catch (SmartlingClientException e) {
            throw e;
        }

        matchThirdPartyTextUnits(stringDataToCheck, assetId, file);
    }

    SourceStringsResponse getSourceStrings(String projectId, String file, Integer offset) throws SmartlingClientException {
        try {
            return smartlingClient.getSourceStrings(projectId, file, offset);
        } catch (SmartlingClientException e) {
            throw e;
        }
    }

    void matchThirdPartyTextUnits(List<StringData> stringDataList, Long assetId, String file) {
        Instant start = Instant.now();
        AtomicInteger fullMatch = new AtomicInteger(0);
        AtomicInteger partialMatch = new AtomicInteger(0);
        AtomicInteger newThirdPartyTextUnit = new AtomicInteger(0);
        AtomicInteger newTPTWithoutMatchingMojitoTU = new AtomicInteger(0);
        stringDataList.forEach(
                stringData ->
                        stringData.getItems().forEach(
                                stringInfo -> {
                                    String hashcode = stringInfo.getHashcode();
                                    String mappingKey = stringInfo.getParsedStringText();
                                    ThirdPartyTextUnit thirdPartyTextUnit = new ThirdPartyTextUnit();
                                    thirdPartyTextUnit.setThirdPartyTextUnitId(hashcode);
                                    thirdPartyTextUnit.setMappingKey(mappingKey);
                                    ThirdPartyTextUnit existingThirdPartyTextUnitFullMatch = thirdPartyTextUnitRepository.findByThirdPartyTextUnitIdAndMappingKey(
                                            hashcode,
                                            mappingKey);

                                    ThirdPartyTextUnit existingThirdPartyTextUnitPartialMatch = thirdPartyTextUnitRepository.findByThirdPartyTextUnitId(
                                            hashcode);

                                    if (existingThirdPartyTextUnitFullMatch != null) {
                                        fullMatch.getAndIncrement();
                                    } else {
                                        if (existingThirdPartyTextUnitPartialMatch != null) {
                                            partialMatch.getAndIncrement();
                                            thirdPartyTextUnitRepository.delete(existingThirdPartyTextUnitPartialMatch);
                                            thirdPartyTextUnitRepository.flush();
                                        } else {
                                            newThirdPartyTextUnit.getAndIncrement();
                                        }
                                        TMTextUnit matchingMojitoTextUnit = tmTextUnitRepository.findFirstByContentAndAssetId(mappingKey, assetId);
                                        if (matchingMojitoTextUnit.equals(null)) {
                                            newTPTWithoutMatchingMojitoTU.incrementAndGet();
                                        }
                                        thirdPartyTextUnit.setTmTextUnit(matchingMojitoTextUnit);
                                        thirdPartyTextUnitRepository.save(thirdPartyTextUnit);
                                    }
                                }
                        ));
        Instant end = Instant.now();
        logger.debug("Hashcodes with existing matching hashcode and mapping key for file {}: {}", file, fullMatch.get());
        logger.debug("Hashcodes with existing matching hashcode but mismatched mapping key for file {}: {}", file, partialMatch.get());
        logger.debug("Hashcodes that are not yet in the database for file {}: {}", file, newThirdPartyTextUnit.get());
        logger.debug("New hashcodes that do not have a matching mojito text unit for file {}: {}", file, newTPTWithoutMatchingMojitoTU);
        logger.debug("hashcode matching start for file {}: {}", file, start);
        logger.debug("hashcode matching end for file {}: {}", file, end);

    }

}
