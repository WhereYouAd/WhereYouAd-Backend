package com.whereyouad.WhereYouAd.infrastructure.client.webpush;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebPushEndpointValidatorTest {

    @Test
    void resolvesAndAcceptsOnlyPublicAddresses() throws Exception {
        InetAddress publicAddress = address("push.example", 8, 8, 8, 8);
        WebPushEndpointValidator validator = new WebPushEndpointValidator(host -> new InetAddress[]{publicAddress});

        WebPushEndpointValidator.ResolvedEndpoint resolved =
                validator.resolveAndValidate("https://push.example/subscriptions/123");

        assertThat(resolved.uri().getHost()).isEqualTo("push.example");
        assertThat(resolved.addresses()).containsExactly(publicAddress);
    }

    @Test
    void rejectsWhenAnyResolvedAddressIsPrivate() throws Exception {
        InetAddress publicAddress = address("push.example", 8, 8, 8, 8);
        InetAddress privateAddress = address("push.example", 10, 0, 0, 1);
        WebPushEndpointValidator validator =
                new WebPushEndpointValidator(host -> new InetAddress[]{publicAddress, privateAddress});

        assertThatThrownBy(() -> validator.resolveAndValidate("https://push.example/subscriptions/123"))
                .isInstanceOf(UnknownHostException.class)
                .hasMessageContaining("non-public");
    }

    @Test
    void rejectsReservedAddressRange() throws Exception {
        InetAddress documentationAddress = address("push.example", 192, 0, 2, 1);
        WebPushEndpointValidator validator =
                new WebPushEndpointValidator(host -> new InetAddress[]{documentationAddress});

        assertThatThrownBy(() -> validator.resolveAndValidate("https://push.example/subscriptions/123"))
                .isInstanceOf(UnknownHostException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "::ffff:10.0.0.1",
            "64:ff9b::a00:1",
            "2002:a00:1::"
    })
    void rejectsIpv4EmbeddingAddressRanges(String address) throws Exception {
        assertThat(WebPushEndpointValidator.isPublicAddress(InetAddress.getByName(address))).isFalse();
    }

    @Test
    void acceptsGlobalIpv6Address() throws Exception {
        assertThat(WebPushEndpointValidator.isPublicAddress(
                InetAddress.getByName("2606:4700:4700::1111"))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://push.example/subscriptions/123",
            "https://user@push.example/subscriptions/123",
            "https://push.example/subscriptions/123#fragment",
            "https:///subscriptions/123"
    })
    void rejectsUnsafeUriForms(String endpoint) {
        assertThat(WebPushEndpointValidator.isValidHttpsUri(endpoint)).isFalse();
    }

    private InetAddress address(String host, int... octets) throws UnknownHostException {
        byte[] bytes = new byte[octets.length];
        for (int i = 0; i < octets.length; i++) {
            bytes[i] = (byte) octets[i];
        }
        return InetAddress.getByAddress(host, bytes);
    }
}
