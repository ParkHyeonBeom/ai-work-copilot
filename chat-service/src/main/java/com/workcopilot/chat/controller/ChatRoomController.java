package com.workcopilot.chat.controller;

import com.workcopilot.chat.dto.*;
import com.workcopilot.chat.service.ChatMessageService;
import com.workcopilot.chat.service.ChatRoomService;
import com.workcopilot.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    @PostMapping
    public ApiResponse<ChatRoomDto> createRoom(Authentication authentication,
                                                @RequestBody ChatRoomCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        String userName = authentication.getName();
        ChatRoomDto room = chatRoomService.createRoom(userId, userName, request);
        return ApiResponse.ok(room);
    }

    @GetMapping
    public ApiResponse<List<ChatRoomDto>> getRooms(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<ChatRoomDto> rooms = chatRoomService.getRooms(userId);
        return ApiResponse.ok(rooms);
    }

    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomDto> getRoom(Authentication authentication,
                                             @PathVariable Long roomId) {
        Long userId = (Long) authentication.getPrincipal();
        ChatRoomDto room = chatRoomService.getRoom(userId, roomId);
        return ApiResponse.ok(room);
    }

    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> leaveRoom(Authentication authentication,
                                        @PathVariable Long roomId) {
        Long userId = (Long) authentication.getPrincipal();
        chatRoomService.leaveRoom(userId, roomId);
        return ApiResponse.ok(null, "채팅방을 나갔습니다.");
    }

    @PostMapping("/{roomId}/invite")
    public ApiResponse<Void> inviteMembers(Authentication authentication,
                                            @PathVariable Long roomId,
                                            @RequestBody InviteMembersRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        chatRoomService.inviteMembers(userId, roomId, request.memberIds());
        return ApiResponse.ok(null, "멤버를 초대했습니다.");
    }

    @GetMapping("/{roomId}/messages")
    public ApiResponse<List<ChatMessageDto>> getMessages(Authentication authentication,
                                                          @PathVariable Long roomId,
                                                          @RequestParam(required = false) Long cursor,
                                                          @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) authentication.getPrincipal();
        List<ChatMessageDto> messages = chatMessageService.getMessages(userId, roomId, cursor, size);
        return ApiResponse.ok(messages);
    }

    @PostMapping("/{roomId}/read")
    public ApiResponse<Void> markAsRead(Authentication authentication,
                                         @PathVariable Long roomId) {
        Long userId = (Long) authentication.getPrincipal();
        chatMessageService.markAsRead(userId, roomId);
        return ApiResponse.ok(null, "읽음 처리되었습니다.");
    }

    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ApiResponse<ChatMessageDto> deleteMessage(Authentication authentication,
                                                      @PathVariable Long roomId,
                                                      @PathVariable Long messageId) {
        Long userId = (Long) authentication.getPrincipal();
        ChatMessageDto deleted = chatMessageService.deleteMessage(userId, messageId);
        return ApiResponse.ok(deleted);
    }
}
