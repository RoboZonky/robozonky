/*
 * Copyright 2020 The RoboZonky Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robozonky.internal.util.functional;

import java.util.NoSuchElementException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class EitherTest {

    @Test
    void gettersWhenRight() {
        Either<Exception, Integer> either = Either.right(100);
        assertSoftly(softly -> {
            softly.assertThat(either.isRight()).isTrue();
            softly.assertThat(either.get()).isEqualTo(100);
            softly.assertThat(either.getOrElse(10)).isEqualTo(100);
            softly.assertThat(either.isLeft()).isFalse();
            softly.assertThatThrownBy(either::getLeft).isInstanceOf(NoSuchElementException.class);
            softly.assertThat(either.getOrElseGet(x -> 10)).isEqualTo(100);
        });
    }

    @Test
    void gettersWhenLeft() {
        Either<Exception, Integer> either = Either.left(new IllegalStateException());
        assertSoftly(softly -> {
            softly.assertThat(either.isRight()).isFalse();
            softly.assertThatThrownBy(either::get).isInstanceOf(NoSuchElementException.class);
            softly.assertThat(either.getOrElse(10)).isEqualTo(10);
            softly.assertThat(either.isLeft()).isTrue();
            softly.assertThat(either.getLeft()).isInstanceOf(IllegalStateException.class);
            softly.assertThat(either.getOrElseGet(x -> 10)).isEqualTo(10);
        });
    }

    @Test
    void mappingWhenLeft() {
        Either<Exception, Integer> either = Either.left(new IllegalStateException());
        Either<Exception, Integer> mapped = either.mapLeft(IllegalArgumentException::new);
        assertSoftly(softly -> {
            softly.assertThat(mapped.getLeft()).isInstanceOf(IllegalArgumentException.class)
                    .hasCauseInstanceOf(IllegalStateException.class);
            softly.assertThatThrownBy(mapped::getRight).isInstanceOf(NoSuchElementException.class);
        });
        Function<Integer, Exception> mapper = mock(Function.class);
        Exception remapped = either.fold(IllegalArgumentException::new, mapper);
        assertThat(remapped).isInstanceOf(IllegalArgumentException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        verify(mapper, never()).apply(anyInt());
    }

    @Test
    void mappingWhenRight() {
        Either<Exception, Integer> either = Either.right(100);
        Either<Exception, Integer> mapped = either.mapRight(a -> a * 2);
        assertSoftly(softly -> {
            softly.assertThat(mapped.getRight()).isEqualTo(200);
            softly.assertThatThrownBy(mapped::getLeft).isInstanceOf(NoSuchElementException.class);
        });
        Function<Exception, Exception> leftMapper = mock(Function.class);
        Function<Integer, Exception> rightMapper = i -> new IllegalStateException();
        Exception remapped = either.fold(leftMapper, rightMapper);
        assertThat(remapped).isInstanceOf(IllegalStateException.class);
        verify(leftMapper, never()).apply(any());
    }

}
