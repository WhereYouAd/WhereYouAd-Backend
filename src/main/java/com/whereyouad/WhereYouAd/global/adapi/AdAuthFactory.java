package com.whereyouad.WhereYouAd.global.adapi;

import com.whereyouad.WhereYouAd.domains.advertisement.domain.constant.Provider;
import com.whereyouad.WhereYouAd.global.adapi.exception.AdApiHandler;
import com.whereyouad.WhereYouAd.global.adapi.exception.code.AdApiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AdAuthFactory {

    private final Map<Provider, AdAuthStrategy> strategies;

    public AdAuthFactory(List<AdAuthStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(AdAuthStrategy::getProvider, Function.identity()));
    }

    public AdAuthStrategy getStrategy(Provider provider) {
        AdAuthStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new AdApiHandler(AdApiErrorCode.INVALID_PROVIDER_VALUE);
        }
        return strategy;
    }
}
