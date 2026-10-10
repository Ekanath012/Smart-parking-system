package com.example.parking.service;

import java.util.Map;

/**
 * Immutable snapshot of a parking event. All values are resolved before asynchronous email work starts.
 */
public record NotificationContent(String title,
                                  String message,
                                  String bookingReference,
                                  String customerName,
                                  String customerEmail,
                                  Map<String, Map<String, String>> sections) {
}
