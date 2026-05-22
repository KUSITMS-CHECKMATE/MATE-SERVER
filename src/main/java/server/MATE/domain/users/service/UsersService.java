package server.MATE.domain.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.MATE.domain.users.dto.response.MeResponse;
import server.MATE.domain.users.entity.Users;
import server.MATE.domain.users.repository.UsersRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.global.config.CacheNames;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository usersRepository;

    @Cacheable(value = CacheNames.USER_SESSION, key = "#userId")
    public MeResponse getMe(Long userId) {
        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new BaseException(BaseErrorCode.AUTH_004));

        return MeResponse.from(user);
    }
}
