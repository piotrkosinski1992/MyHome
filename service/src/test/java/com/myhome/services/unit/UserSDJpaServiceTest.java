/*
 * Copyright 2020 Prathab Murugan
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

package com.myhome.services.unit;

import com.myhome.services.springdatajpa.exceptions.EmailAlreadyExistsException;
import com.myhome.services.springdatajpa.exceptions.UserNotFoundException;
import helpers.TestUtils;
import com.myhome.controllers.dto.UserDto;
import com.myhome.controllers.dto.mapper.UserMapper;
import com.myhome.domain.Community;
import com.myhome.domain.User;
import com.myhome.repositories.UserRepository;
import com.myhome.services.springdatajpa.UserSDJpaService;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class UserSDJpaServiceTest {

  private final String USER_ID = "test-user-id";
  private final String USERNAME = "test-user-id";
  private final String USER_EMAIL = "test-user-id";
  private final String USER_PASSWORD = "test-user-id";

  @Mock
  private UserRepository userRepository;
  @Mock
  private UserMapper userMapper;
  @Mock
  private PasswordEncoder passwordEncoder;
  @InjectMocks
  private UserSDJpaService userService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void createUserSuccess() {
    // given
    UserDto request = getDefaultUserDtoRequest();
    User resultUser = getUserFromDto(request);
    UserDto response = UserDto.builder()
        .id(resultUser.getId())
        .userId(resultUser.getUserId())
        .name(resultUser.getName())
        .encryptedPassword(resultUser.getEncryptedPassword())
        .communityIds(new HashSet<>())
        .build();

    given(userRepository.findByEmail(request.getEmail()))
        .willReturn(Optional.empty());
    given(passwordEncoder.encode(request.getPassword()))
        .willReturn(request.getPassword());
    given(userMapper.userDtoToUser(request))
        .willReturn(resultUser);
    given(userRepository.save(resultUser))
        .willReturn(resultUser);
    given(userMapper.userToUserDto(resultUser))
        .willReturn(response);

    // when
    userService.createUser(request);

    // then
    verify(userRepository).save(User.builder()
    .name("test-user-id")
    .userId("test-user-id")
    .email("test-user-id")
    .encryptedPassword("test-user-id")
    .communities(new HashSet<>())
    .build());
    verify(userRepository).existsByEmail(request.getEmail());
    verify(passwordEncoder).encode(request.getPassword());
    verify(userRepository).save(resultUser);
  }

  @Test
  void createUserEmailExists() {
    // given
    UserDto request = getDefaultUserDtoRequest();
    User user = getUserFromDto(request);

    given(userRepository.existsByEmail(request.getEmail()))
        .willReturn(true);

    // when
    Exception exception = Assertions.assertThrows(
        EmailAlreadyExistsException.class,
        () -> userService.createUser(request));

    // then
    assertEquals(exception.getMessage(), "Email already exists:419");
  }

  @Test
  void getUserDetailsSuccess() {
    // given
    UserDto userDto = getDefaultUserDtoRequest();
    User user = getUserFromDto(userDto);

    given(userRepository.findByUserIdWithCommunities(USER_ID))
        .willReturn(Optional.of(user));
    given(userMapper.userToUserDto(user))
        .willReturn(userDto);

    // when
    UserDto createdUserDto = userService.getUserDetails(USER_ID);

    // then
    assertNotNull(createdUserDto);
    assertEquals(userDto, createdUserDto);
    assertEquals(2, createdUserDto.getCommunityIds().size());
    verify(userRepository).findByUserIdWithCommunities(USER_ID);
  }

  @Test
  void getUserDetailsSuccessWithCommunityIds() {
    // given
    UserDto userDto = getDefaultUserDtoRequest();
    User user = new User(userDto.getName(), userDto.getUserId(), userDto.getEmail(),
        userDto.getEncryptedPassword(), new HashSet<>());

    Community firstCommunity = TestUtils.CommunityHelpers.getTestCommunity(user);
    Community secCommunity = TestUtils.CommunityHelpers.getTestCommunity(user);

    Set<Community> communities =
        Stream.of(firstCommunity, secCommunity).collect(Collectors.toSet());

    Set<String> communitiesIds = communities
        .stream()
        .map(Community::getCommunityId)
        .collect(Collectors.toSet());

    given(userRepository.findByUserIdWithCommunities(USER_ID))
        .willReturn(Optional.of(user));
    given(userMapper.userToUserDto(user))
        .willReturn(userDto);

    // when
    UserDto createdUserDto = userService.getUserDetails(USER_ID);

    // then
    assertNotNull(createdUserDto);
    assertEquals(userDto, createdUserDto);
    assertEquals(communitiesIds.size(), createdUserDto.getCommunityIds().size());
    verify(userRepository).findByUserIdWithCommunities(USER_ID);
  }

  @Test
  void getUserDetailsNotFound() {
    // given
    given(userRepository.findByUserIdWithCommunities(USER_ID))
        .willReturn(Optional.empty());

    // when
    Exception exception = Assertions.assertThrows(
        UserNotFoundException.class,
        () -> userService.getUserDetails(USER_ID));
  }

  @Test
  void listAll() {
    given(userRepository.findAll(PageRequest.of(0, 200)))
        .willReturn(Page.empty());

    // when
    Set<User> result = userService.listAll();

    //then
    assertEquals(0, result.size());
  }

  @Test
  void listAllWithPagination() {
    PageRequest pageRequest = PageRequest.of(0, 200);
    given(userRepository.findAll(pageRequest))
        .willReturn(Page.empty());

    // when
    Set<User> result = userService.listAll(pageRequest);

    //then
    assertEquals(0, result.size());
  }

  private UserDto getDefaultUserDtoRequest() {
    return UserDto.builder()
        .userId(USER_ID)
        .name(USERNAME)
        .email(USER_EMAIL)
        .encryptedPassword(USER_PASSWORD)
        .communityIds(new HashSet<String>() {{
          add("5168673e-b47d-47ac-808f-2a473ca58f7e");
          add("d87a43a5-610e-4d8d-81d1-cd82f7464de4");
        }})
        .build();
  }

  private User getUserFromDto(UserDto request) {
    return new User(
        request.getName(),
        request.getUserId(),
        request.getEmail(),
        request.getEncryptedPassword(),
        new HashSet<>()
    );
  }
}
