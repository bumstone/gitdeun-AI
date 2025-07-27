package com.hyetaekon.hyetaekon.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 인증 관련
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "AUTH-001", "비밀번호가 일치하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH-002", "리프레시 토큰이 만료되었습니다."),
    NO_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "토큰이 존재하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-004", "유효하지 않은 토큰입니다."),
    ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "AUTH-005", "인증되지 않은 유저입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-006", "유효하지 않은 사용자 이름 또는 비밀번호입니다."),
    INVALID_SECRET_KEY(HttpStatus.UNAUTHORIZED, "AUTH-007", "유효하지 않은 비밀 키입니다."),
    DELETE_USER_DENIED(HttpStatus.FORBIDDEN, "AUTH-008", "회원 탈퇴가 거부되었습니다."),
    CANNOT_REPORT_SELF(HttpStatus.BAD_REQUEST, "AUTH-009","자기 자신을 신고할 수 없습니다."),
    ROLE_NOT_FOUND(HttpStatus.FORBIDDEN, "AUTH-010", "권한 정보가 없습니다."),
    BLACKLIST_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-011", "사용할 수 없는 액세스 토큰입니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH-012", "해당 신고 내역을 찾을 수 없습니다."),
    REPORT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "AUTH-013", "이미 처리된 신고입니다."),
    INVALID_REPORT_REQUEST(HttpStatus.BAD_REQUEST, "AUTH-014","잘못된 신고 요청입니다."),

    // 계정 관련
    DUPLICATED_REAL_ID(HttpStatus.CONFLICT, "ACCOUNT-001", "이미 존재하는 아이디입니다."),
    USER_NOT_FOUND_BY_REAL_ID(HttpStatus.NOT_FOUND, "ACCOUNT-002", "해당 아이디의 회원을 찾을 수 없습니다."),
    USER_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND, "ACCOUNT-003", "해당 아이디의 회원을 찾을 수 없습니다."),
    DUPLICATED_NICKNAME(HttpStatus.CONFLICT, "ACCOUNT-004", "이미 사용 중인 닉네임입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "ACCOUNT-005", "현재 비밀번호가 일치하지 않습니다."),
    PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "ACCOUNT-006", "새로운 비밀번호는 현재 비밀번호와 달라야 합니다."),
    CURRENT_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "ACCOUNT-007", "현재 비밀번호를 입력해야 합니다."),
    FORBIDDEN_ACCESS(HttpStatus.FORBIDDEN, "AUTH-011", "잘못된 접근입니다."),
    NOT_SUSPENDED_USER(HttpStatus.BAD_REQUEST, "ACCOUNT-008", "정지 상태가 아닌 회원입니다."),
    INVALID_SUSPEND_TIME(HttpStatus.BAD_REQUEST, "ACCOUNT-009", "정지 기간이 유효하지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "ACCOUNT-010", "새 비밀번호와 확인 비밀번호가 일치하지 않습니다."),

    // 북마크
    BOOKMARK_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK-001", "북마크한 유저를 찾을 수 없습니다."),
    BOOKMARK_ALREADY_EXISTS(HttpStatus.CONFLICT, "BOOKMARK-002", "이미 북마크한 서비스입니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOKMARK-003", "북마크 정보를 찾을 수 없습니다."),

    // 좋아요
    RECOMMEND_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECOMMEND-001", "이미 좋아요를 누른 게시글입니다."),
    RECOMMEND_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMEND-002", "좋아요 정보를 찾을 수 없습니다."),
    RECOMMEND_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMEND-003", "추천한 유저를 찾을 수 없습니다."),

    // 게시글
    POST_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND,"POST-001", "해당 아이디의 게시글을 찾을 수 없습니다"),

    // 답변
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "ANSWER-001","답변을 찾을 수 없습니다."),
    ANSWER_NOT_MATCHED_POST(HttpStatus.BAD_REQUEST, "ANSWER-002","해당 게시글에 속하지 않는 답변입니다."),

    // 관심사 선택 제한
    INTEREST_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "INTEREST-001", "관심사는 최대 6개까지만 등록 가능합니다."),
    INVALID_INTEREST(HttpStatus.BAD_REQUEST, "INTEREST-002", "유효하지 않은 관심사입니다."),

    // 공공서비스
    // 유효 JACODE 확인
    INVALID_ENUM_CODE(HttpStatus.BAD_REQUEST, "SERVICE-001", "유효하지 않은 코드 값입니다."),
    INCOMPLETE_SERVICE_DETAIL(HttpStatus.BAD_REQUEST, "SERVICE-002","서비스 상세 정보가 불완전합니다."),

    SERVICE_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "SERVICE-001", "해당 서비스 분야를 찾을 수 없습니다."),
    SERVICE_NOT_FOUND_BY_ID(HttpStatus.NOT_FOUND,"SERVICE-002", "해당 아이디의 서비스를 찾을 수 없습니다"),

    // PublicServiceData 관련
    INVALID_PUBLIC_SERVICE_DATA(HttpStatus.BAD_REQUEST, "PUBLIC_SERVICE-001", "유효하지 않은 공공서비스 데이터입니다."),
    PUBLIC_SERVICE_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PUBLIC_SERVICE-002", "공공서비스 데이터 조회 중 오류가 발생했습니다."),
    PUBLIC_SERVICE_URI_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PUBLIC_SERVICE-003", "공공서비스 API URI 구문 오류가 발생했습니다."),
    PUBLIC_SERVICE_BAD_REQUEST(HttpStatus.BAD_REQUEST, "PUBLIC_SERVICE-004", "공공서비스 API 요청이 잘못되었습니다."),
    PUBLIC_SERVICE_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PUBLIC_SERVICE-005", "공공서비스 API 서버 오류가 발생했습니다."),
    PUBLIC_SERVICE_DATA_MAPPING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PUBLIC_SERVICE-006", "공공서비스 데이터 매핑 중 오류가 발생했습니다."),
    PUBLIC_SERVICE_NETWORK_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PUBLIC_SERVICE-007", "공공서비스 API 네트워크 오류가 발생했습니다."),

    // S3 파일 관련
    FILE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-001", "업로드 가능한 파일 개수를 초과했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FILE-002", "파일 크기가 허용된 용량을 초과했습니다."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "FILE-003", "지원하지 않는 파일 형식입니다."),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FILE-004", "요청한 파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-006", "파일 업로드 중 오류가 발생했습니다."),
    INVALID_FILE_LIST(HttpStatus.BAD_REQUEST, "FILE-006", "파일 목록이 비어있거나 유효하지 않습니다."),
    INVALID_FILE_PATH(HttpStatus.BAD_REQUEST, "FILE-007", "파일 경로나 이름이 유효하지 않습니다."),
    FILE_MOVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FILE-008", "파일 이동 중 오류가 발생했습니다.");

    

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}