package com.whereyouad.WhereYouAd.infrastructure.client.webpush;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class WebPushEndpointValidator {

    private static final List<Subnet> NON_PUBLIC_IPV4_RANGES = List.of(
            Subnet.of("0.0.0.0", 8),
            Subnet.of("10.0.0.0", 8),
            Subnet.of("100.64.0.0", 10),
            Subnet.of("127.0.0.0", 8),
            Subnet.of("169.254.0.0", 16),
            Subnet.of("172.16.0.0", 12),
            Subnet.of("192.0.0.0", 24),
            Subnet.of("192.0.2.0", 24),
            Subnet.of("192.88.99.0", 24),
            Subnet.of("192.168.0.0", 16),
            Subnet.of("198.18.0.0", 15),
            Subnet.of("198.51.100.0", 24),
            Subnet.of("203.0.113.0", 24),
            Subnet.of("224.0.0.0", 4),
            Subnet.of("240.0.0.0", 4)
    );

    private static final List<Subnet> NON_PUBLIC_IPV6_RANGES = List.of(
            Subnet.of("::", 80),
            Subnet.of("64:ff9b::", 96),
            Subnet.of("64:ff9b:1::", 48),
            Subnet.of("100::", 64),
            Subnet.of("2001::", 32),
            Subnet.of("2001:2::", 48),
            Subnet.of("2001:db8::", 32),
            Subnet.of("2002::", 16),
            Subnet.of("fc00::", 7),
            Subnet.of("fe80::", 10),
            Subnet.of("ff00::", 8)
    );

    private final HostResolver hostResolver;

    public WebPushEndpointValidator() {
        this(InetAddress::getAllByName);
    }

    WebPushEndpointValidator(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    public ResolvedEndpoint resolveAndValidate(String endpoint) throws UnknownHostException {
        URI uri = parseHttpsUri(endpoint);
        InetAddress[] addresses = hostResolver.resolve(uri.getHost());
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException("Web Push endpoint host has no address");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new UnknownHostException("Web Push endpoint resolves to a non-public address");
            }
        }
        return new ResolvedEndpoint(uri, addresses.clone());
    }

    public static boolean isValidHttpsUri(String endpoint) {
        try {
            parseHttpsUri(endpoint);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        List<Subnet> blockedRanges = bytes.length == 4 ? NON_PUBLIC_IPV4_RANGES : NON_PUBLIC_IPV6_RANGES;
        return blockedRanges.stream().noneMatch(subnet -> subnet.contains(bytes));
    }

    private static URI parseHttpsUri(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Web Push endpoint is required");
        }
        try {
            URI uri = new URI(endpoint);
            if (!uri.isAbsolute()
                    || !"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getRawUserInfo() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Web Push endpoint must be an absolute HTTPS URI");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Web Push endpoint URI", e);
        }
    }

    record ResolvedEndpoint(URI uri, InetAddress[] addresses) {
        ResolvedEndpoint {
            addresses = addresses.clone();
        }

        @Override
        public InetAddress[] addresses() {
            return addresses.clone();
        }
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    private record Subnet(byte[] network, int prefixLength) {

        static Subnet of(String network, int prefixLength) {
            try {
                return new Subnet(InetAddress.getByName(network).getAddress(), prefixLength);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Invalid static subnet: " + network, e);
            }
        }

        boolean contains(byte[] address) {
            if (address.length != network.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (address[i] != network[i]) {
                    return false;
                }
            }
            if (remainingBits == 0) {
                return true;
            }
            int mask = 0xFF << (8 - remainingBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
