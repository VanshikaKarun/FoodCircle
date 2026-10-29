package com.vkarun.foodcircle.service;

import com.vkarun.foodcircle.common.ApiException;
import com.vkarun.foodcircle.entity.OsmType;
import com.vkarun.foodcircle.entity.Place;
import com.vkarun.foodcircle.entity.PlaceCategory;
import com.vkarun.foodcircle.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Limit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// Plain unit test: the repository is a mock, so there is no Spring and no database.
class PlaceServiceTest {

    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceService placeService = new PlaceService(placeRepository);

    @Test
    void likePattern_wrapsTheTextInPercentSigns() {
        assertThat(PlaceService.likePattern("paradise")).isEqualTo("%paradise%");
    }

    @Test
    void likePattern_escapesTheCharactersThatAreWildcardsInLike() {
        assertThat(PlaceService.likePattern("50%")).isEqualTo("%50\\%%");
        assertThat(PlaceService.likePattern("a_b")).isEqualTo("%a\\_b%");
    }

    @Test
    void likePattern_escapesTheBackslashFirst_soTheAddedBackslashesAreNotDoubled() {
        // one typed backslash followed by a percent sign: \%  ->  \\ (literal backslash) then \% (literal percent)
        assertThat(PlaceService.likePattern("\\%")).isEqualTo("%\\\\\\%%");
    }

    @Test
    void searchByName_trimsTheText_andPassesThePatternAndLimitToTheRepository() {
        Place place = new Place(OsmType.NODE, 1L, "Hotel Paradise", PlaceCategory.RESTAURANT, null, 17.4, 78.4, null);
        given(placeRepository.findOpenByNamePattern(eq("%para%"), any(Limit.class))).willReturn(List.of(place));

        var result = placeService.searchByName("  para  ", 5);

        assertThat(result).extracting(r -> r.name()).containsExactly("Hotel Paradise");
        verify(placeRepository).findOpenByNamePattern("%para%", Limit.of(5));
    }

    @Test
    void searchByName_textThatIsTooShortAfterTrimming_isRejectedWithoutTouchingTheDatabase() {
        assertThatThrownBy(() -> placeService.searchByName("  a ", 20))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("at least 2");
        verifyNoInteractions(placeRepository);
    }
}
