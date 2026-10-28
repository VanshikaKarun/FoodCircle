package com.vkarun.foodcircle.dto;

import java.util.List;

// One page of a long list. nextCursor is sent back as ?cursor= to get the following page; null means this was the last page.
public record CursorPage<T>(List<T> items, String nextCursor) {
}
