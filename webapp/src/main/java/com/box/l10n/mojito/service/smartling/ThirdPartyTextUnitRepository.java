package com.box.l10n.mojito.service.smartling;

import com.box.l10n.mojito.entity.ThirdPartyTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ThirdPartyTextUnitRepository extends JpaRepository<ThirdPartyTextUnit, Long>, JpaSpecificationExecutor<ThirdPartyTextUnit> {

    ThirdPartyTextUnit findByThirdPartyTextUnitIdAndMappingKey(String thirdPartyTextUnit, String mappingKey);

    ThirdPartyTextUnit findByThirdPartyTextUnitId(String thirdPartyTextUnit);

}
