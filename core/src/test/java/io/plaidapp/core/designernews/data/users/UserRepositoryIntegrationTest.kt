/*
 * Copyright 2018 Google, Inc.
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

package io.plaidapp.core.designernews.data.users

import io.plaidapp.core.data.Result
import io.plaidapp.core.designernews.data.api.DesignerNewsService
import io.plaidapp.core.designernews.data.api.errorResponseBody
import io.plaidapp.core.designernews.data.api.model.User
import io.plaidapp.core.designernews.data.api.user1
import io.plaidapp.core.designernews.data.api.user2
import io.plaidapp.core.designernews.data.api.users
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import retrofit2.Response

/**
 * Test for [UserRepository] where only the service is mocked.
 */
class UserRepositoryIntegrationTest {

    private val service = Mockito.mock(DesignerNewsService::class.java)
    private val dataSource = UserRemoteDataSource(service)
    private val repository = UserRepository(dataSource)

    @Test
    fun getUsers_withNoCachedUsers_withSuccess() = runBlocking {
        // Given that the service responds with success
        withUsersSuccess("111,222", users)

        // When requesting the users
        val result = repository.getUsers(setOf(111L, 222L))

        // Then there's one request to the service
        Mockito.verify(service).getUsers("111,222")
        // Then the correct set of users is returned
        assertEquals(Result.Success(users.toSet()), result)
    }

    @Test
    fun getUsers_withNoCachedUsers_withError() = runBlocking {
        // Given that the service responds with error
        withUsersError("111,222")

        // When requesting the users
        val result = repository.getUsers(setOf(111L, 222L))

        // Then error is returned
        assertTrue(result is Result.Error)
    }

    @Test
    fun getUsers_withCachedUsers_withSuccess() = runBlocking {
        // Given a user that was already requested and cached
        withUsersSuccess("111", listOf(user1))
        repository.getUsers(setOf(111L))
        // Given another user that can be requested
        withUsersSuccess("222", listOf(user2))

        // When requesting a list of users
        val result = repository.getUsers(setOf(111L, 222L))
        // Then there's one request to the service
        Mockito.verify(service).getUsers("222")
        // Then the correct set of users is returned
        assertEquals(Result.Success(users.toSet()), result)
    }

    @Test
    fun getUsers_withCachedUsers_withError() = runBlocking {
        // Given a user that was already requested and cached
        withUsersSuccess("111", listOf(user1))
        repository.getUsers(setOf(111L))
        // Given that the service responds with error for another users
        withUsersError("222")

        // When requesting the users
        val result = repository.getUsers(setOf(111L, 222L))

        // We get the cached user
        assertEquals(Result.Success(setOf(user1)), result)
    }

    private fun withUsersSuccess(ids: String, users: List<User>) {
        val result = Response.success(users)
        Mockito.`when`(service.getUsers(ids)).thenReturn(CompletableDeferred(result))
    }

    private fun withUsersError(ids: String) {
        val result = Response.error<List<User>>(400, errorResponseBody)
        Mockito.`when`(service.getUsers(ids)).thenReturn(CompletableDeferred(result))
    }
}
