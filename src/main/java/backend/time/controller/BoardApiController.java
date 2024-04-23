package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.BoardDistanceDto;
import backend.time.dto.BoardListResponseDto;
import backend.time.dto.ResponseDto;
import backend.time.dto.request.BoardDto;
import backend.time.dto.request.BoardSearchDto;
import backend.time.dto.request.BoardUpdateDto;
import backend.time.dto.request.PointDto;
import backend.time.model.Member;
import backend.time.model.Scrap;
import backend.time.model.board.*;
import backend.time.repository.BoardRepository;
import backend.time.repository.MemberRepository;
import backend.time.repository.ScrapRepository;
import backend.time.service.BoardService;
import jakarta.validation.Valid;
import jdk.jfr.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class BoardApiController {

    final private BoardService boardService;
    final private BoardRepository boardRepository;
    final private MemberRepository memberRepository;
    private final ScrapRepository scrapRepository;

    //user 위치 넣기
    @PostMapping("/api/auth/point")
    public ResponseDto<String> addPoint(@RequestBody @Valid PointDto pointDto, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
        boardService.point(pointDto, principalDetail.getMember());
        return new ResponseDto<String>(HttpStatus.OK.value(),"위치 설정 성공");
    }

//    //user 위치 넣기 test
//    @PostMapping("/api/auth/point")
//    public ResponseDto<String> addPoint(@RequestBody @Valid PointDto pointDto) throws IOException {
//        Member member = memberRepository.findById(1L)
//                .orElseThrow(()->new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));
//        boardService.point(pointDto, member);
//        return new ResponseDto<String>(HttpStatus.OK.value(),"위치 설정 성공");
//    }

//    //게시글 작성
//    @PostMapping("/api/auth/board")
//    public ResponseDto<String> writeBoard(@ModelAttribute @Valid BoardDto boardDto, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
//        boardService.write(boardDto, principalDetail.getMember());
//        return new ResponseDto<String>(HttpStatus.OK.value(),"게시글 작성 완료");
//    }

    //게시글 작성 test
    @PostMapping("/api/auth/board")
    public ResponseDto<String> writeBoard(@ModelAttribute @Valid BoardDto boardDto) throws IOException {
        Member member = memberRepository.findById(1L)
                .orElseThrow(()->new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));
        boardService.write(boardDto, member);
        return new ResponseDto<String>(HttpStatus.OK.value(),"게시글 작성 완료");
    }

    //글 조회(검색)
    @GetMapping("/api/board")
    public Result findAll(@ModelAttribute @Valid BoardSearchDto requestDto, @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Page<Board> boards = boardService.searchBoards(requestDto, principalDetail.getMember());

        // BoardDistanceDto 리스트를 생성
        List<BoardDistanceDto> boardDistanceDtos = boardRepository.findNearbyOrUnspecifiedLocationBoardsWithDistance(principalDetail.getMember().getLongitude(), principalDetail.getMember().getLatitude());
        // id를 key로 distance를 값으로 매핑
        Map<Long, Double> boardIdToDistanceMap = boardDistanceDtos.stream()
                .collect(Collectors.toMap(BoardDistanceDto::getId, BoardDistanceDto::getDistance));

        UserAddressResponseDto userAddressResponseDto = new UserAddressResponseDto();
        userAddressResponseDto.setUserLongitude(principalDetail.getMember().getLongitude());
        userAddressResponseDto.setUserLatitude(principalDetail.getMember().getLatitude());
        userAddressResponseDto.setAddress(principalDetail.getMember().getAddress());
        // 결과 DTO 리스트를 생성
        List<BoardListResponseDto> collect = boards.getContent().stream().map(board -> {
            BoardListResponseDto dto = new BoardListResponseDto();
            dto.setBoardId(board.getId());
            dto.setTitle(board.getTitle());
            dto.setCreatedDate(board.getCreateDate());
            dto.setChatCount(board.getChatCount());
            dto.setScrapCount(board.getScrapCount());
            dto.setBoardState(board.getBoardState());
            dto.setDistance(boardIdToDistanceMap.getOrDefault(board.getId(), null));
            if(board.getAddress() !=null) {
            dto.setAddress(board.getAddress());
            }
            //이미지가 있으면 첫번째 사진의 storedFileName 넘겨줌 없으면 null
            if (!board.getImages().isEmpty()) {
                dto.setFirstImage(board.getImages().get(0).getStoredFileName());
            }

            return dto;
        }).collect(Collectors.toList());

        BoardResponseWrapper responseWrapper = new BoardResponseWrapper();
        responseWrapper.setUserAddress(userAddressResponseDto);
        responseWrapper.setBoards(collect);

        return new Result<>(responseWrapper);
    }

//    //글 조회(검색) Test
//    @GetMapping("/api/board")
//    public Result findAll(@ModelAttribute @Valid BoardSearchDto requestDto) {
//        System.out.println(requestDto.getPageNum());
//        System.out.println(requestDto.getKeyword());
//        System.out.println(requestDto.getCategory());
//        Member member = memberRepository.findById(1L)
//                .orElseThrow(()->new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));
//        Page<Board> boards = boardService.searchBoards(requestDto, member);
//
//        // BoardDistanceDto 리스트를 생성
//        List<BoardDistanceDto> boardDistanceDtos = boardRepository.findNearbyOrUnspecifiedLocationBoardsWithDistance(member.getLongitude(), member.getLatitude());
//
//        // id를 key로 distance를 값으로 매핑
//        Map<Long, Double> boardIdToDistanceMap = boardDistanceDtos.stream()
//                .collect(Collectors.toMap(BoardDistanceDto::getId, BoardDistanceDto::getDistance));
//
//        UserAddressResponseDto userAddressResponseDto = new UserAddressResponseDto();
//        userAddressResponseDto.setUserLongitude(member.getLongitude());
//        userAddressResponseDto.setUserLatitude(member.getLatitude());
//        userAddressResponseDto.setAddress(member.getAddress());
//        // 결과 DTO 리스트를 생성
//        List<BoardListResponseDto> collect = boards.getContent().stream().map(board -> {
//            BoardListResponseDto dto = new BoardListResponseDto();
//            dto.setBoardId(board.getId());
//            dto.setTitle(board.getTitle());
//            dto.setCreatedDate(board.getCreateDate());
//            dto.setChatCount(board.getChatCount());
//            dto.setScrapCount(board.getScrapCount());
//            dto.setBoardState(board.getBoardState());
//            dto.setDistance(boardIdToDistanceMap.getOrDefault(board.getId(), null));
//            if(board.getAddress() !=null) {
//                dto.setAddress(board.getAddress());
//            }
//            //이미지가 있으면 첫번째 사진의 storedFileName 넘겨줌 없으면 null
//            if (!board.getImages().isEmpty()) {
//                dto.setFirstImage(board.getImages().get(0).getStoredFileName());
//            }
//            return dto;
//        }).collect(Collectors.toList());
//
//        BoardResponseWrapper responseWrapper = new BoardResponseWrapper();
//        responseWrapper.setUserAddress(userAddressResponseDto);
//        responseWrapper.setBoards(collect);
//
//        return new Result<>(responseWrapper);
//    }

    //글 상세보기
    @GetMapping("/api/board/{id}")
    public Result boardDetail(@PathVariable("id") Long id, @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Board board = boardRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("해당 글이 존재하지 않습니다."));
        BoardDetailResponseDto boardDetailResponseDto = new BoardDetailResponseDto();
        boardDetailResponseDto.setBoardId(board.getId());
        Optional<Scrap> scrap = scrapRepository.findByMemberIdAndBoardId(principalDetail.getMember().getId(), id);
        if(scrap.isEmpty()){
            boardDetailResponseDto.setScrapStus("NO");
        }
        else {
            boardDetailResponseDto.setScrapStus("YES");
        }
        boardDetailResponseDto.setUserId(board.getMember().getId());
        boardDetailResponseDto.setNickname(board.getMember().getNickname());
        boardDetailResponseDto.setMannerTime(board.getMember().getMannerTime());
        boardDetailResponseDto.setTitle(board.getTitle());
        boardDetailResponseDto.setContent(board.getContent());
        boardDetailResponseDto.setCreatedDate(board.getCreateDate());
        boardDetailResponseDto.setChatCount(board.getChatCount());
        boardDetailResponseDto.setScrapCount(board.getScrapCount());
        boardDetailResponseDto.setAddress(board.getAddress());
        boardDetailResponseDto.setLongitude(board.getLongitude());
        boardDetailResponseDto.setLatitude(board.getLatitude());
        boardDetailResponseDto.setBoardState(board.getBoardState());
        boardDetailResponseDto.setCategory(board.getCategory());
        boardDetailResponseDto.setBoardType(board.getBoardType());
        List<Image> images = board.getImages();
        List<String> collect = images.stream().map(Image::getStoredFileName)
                .toList();
        boardDetailResponseDto.setImages(collect);

        return new Result<>(boardDetailResponseDto);
    }

//    //게시글 수정
//    @PutMapping("/api/auth/board/{id}")
//    public ResponseDto<String> updateBoard(@PathVariable("id") Long id, @ModelAttribute @Valid BoardUpdateDto boardUpdateDto, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
//        Board board = boardRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));
//
//        if(!Objects.equals(principalDetail.getMember().getId(), board.getMember().getId())){
//            throw new IllegalArgumentException("잘못된 접근입니다.");
//        }
//
//        boardService.update(id, boardUpdateDto);
//        return new ResponseDto<String>(HttpStatus.OK.value(),"게시글 수정 완료");
//    }

    //게시글 수정 test
    @PutMapping("/api/auth/board/{id}")
    public ResponseDto<String> updateBoard(@PathVariable("id") Long id, @ModelAttribute @Valid BoardUpdateDto boardUpdateDto) throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));

        Member member = memberRepository.findById(1L)
                .orElseThrow(()->new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));

        if(!Objects.equals(member.getId(), member.getId())){
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }

        boardService.update(id, boardUpdateDto);
        return new ResponseDto<String>(HttpStatus.OK.value(),"게시글 수정 완료");
    }

//    //게시글 삭제
//    @DeleteMapping("/api/auth/board/{id}")
//    public ResponseDto<String> deleteBoard(@PathVariable("id") Long id, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
//        Board board = boardRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));
//
//        if(!Objects.equals(principalDetail.getMember().getId(), board.getMember().getId())){
//            throw new IllegalArgumentException("잘못된 접근입니다.");
//        }
//
//        boardService.delete(id);
//        return new ResponseDto<String>(HttpStatus.OK.value(),"게시글 삭제 완료");
//    }

    //게시글 삭제 test
    @DeleteMapping("/api/auth/board/{id}")
    public ResponseDto<String> deleteBoard(@PathVariable("id") Long id) throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));

        Member member = memberRepository.findById(1L)
                .orElseThrow(()->new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));

        if(!Objects.equals(member.getId(), board.getMember().getId())){
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }

        boardService.delete(id);
        return new ResponseDto<String>(HttpStatus.OK.value(),"게시글 삭제 완료");
    }

    @Data
    public class BoardDetailResponseDto{
        //boardId 게시글 식별자
        private Long boardId;
        private String scrapStus;
        //글쓴 사람 닉네임, 틈새시간
        private Long userId;
        private String nickname;
        private Long mannerTime;
        //글의 기본 정보 (제목,내용,글쓴날짜)
        private String title;
        private String content;
        private Timestamp createdDate;
        //채팅수, 스크랩수
        private int chatCount;
        private int scrapCount;
        //글에 담겨있는 주소 정보 (주소, 경도, 위도)
        private String address;
        private Double longitude;
        private Double latitude;
        //board 가테고리, state, type
        private BoardState boardState;
        private BoardCategory category;
        private BoardType boardType;
        //이미지들
        private List<String> images;
    }

    @Data
    public class UserAddressResponseDto{
        private Double userLongitude;
        private Double userLatitude;
        private String address;
    }
//    @Data
//    public class BoardListResponseDto {
//        private Long boardId;
//        private String title;
//        private Timestamp createdDate;
//        private int chatCount;
//        private int scrapCount;
//        private Double distance;
//        private String address;
//        private BoardState boardState;
//        private String firstImage;
//        }

    @Data
    public class BoardResponseWrapper {
        private UserAddressResponseDto userAddress;
        private List<BoardListResponseDto> boards;
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
