package com.box.l10n.mojito.service.smartling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ThirdPartyTextUnitException extends Exception{
    public ThirdPartyTextUnitException(String message) {
        super(message);
    }
}
