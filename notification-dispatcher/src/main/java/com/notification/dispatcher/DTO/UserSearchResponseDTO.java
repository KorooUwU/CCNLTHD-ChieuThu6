package com.notification.dispatcher.DTO;

import java.util.List;

public record UserSearchResponseDTO(List<UserSummaryDTO> content, int page, int size,
                                    long totalElements, int totalPages) {}
