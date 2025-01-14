package com.effourt.calenkit.controller;

import com.effourt.calenkit.domain.Alarm;
import com.effourt.calenkit.domain.Member;
import com.effourt.calenkit.dto.EmailMessage;
import com.effourt.calenkit.dto.TeamShare;
import com.effourt.calenkit.exception.ExistsTeamException;
import com.effourt.calenkit.exception.MemberNotFoundException;
import com.effourt.calenkit.exception.ScheduleNotFoundException;
import com.effourt.calenkit.exception.TeamNotFoundException;
import com.effourt.calenkit.repository.AlarmRepository;
import com.effourt.calenkit.repository.MemberRepository;
import com.effourt.calenkit.service.AlarmService;
import com.effourt.calenkit.service.TeamScheduleService;
import com.effourt.calenkit.util.EmailSend;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TeamController {

    private final TeamScheduleService teamScheduleService;
    private final AlarmService alarmService;

    private final MemberRepository memberRepository;

    private final MessageSource ms;
    private final EmailSend emailSend;

    /**
     * 회원 조회 (실시간 회원 검색)
     * @param memId
     */
    // http://localhost:8080/members?memId=Test3@test3.com
    @GetMapping("/members")
    @ResponseBody
    public Member searchMemberForShare(@RequestParam String memId) {
        return memberRepository.findByMemId(memId);
    }

    /**
     * 동행 조회
     * */
    // http://localhost:8080/teams/share/1
    @GetMapping ("/teams/share/{scNo}")
    @ResponseBody
    public List<TeamShare> searchMyTeam(@PathVariable int scNo) throws TeamNotFoundException, ScheduleNotFoundException{
        return teamScheduleService.getTeam(scNo);
    }

    /**
     * 동행 추가 (동행추가 + 알람서비스)
     * */
    // http://localhost:8080/teams/share/1  : memId=test@test.com
    @PostMapping ("/teams/share/{scNo}")
    @ResponseBody
    public String shareTeam(@PathVariable int scNo, @RequestBody Map<String,Object> map, HttpSession session) throws ExistsTeamException, MemberNotFoundException, ScheduleNotFoundException{
        String loginId = (String) session.getAttribute("loginId");
        String memId = (String)map.get("memId");

        log.info("[shareTeam] scNo = {}",scNo);
        log.info("[shareTeam] memId = {}",memId);
        log.info("[shareTeam] loginId = {}",loginId);

        if(loginId.equals(memId)){
            teamScheduleService.addTeam(scNo,memId);
            Alarm alarm = alarmService.addAlarmBySaveTeam(scNo,memId); //알람서비스
            log.info("[shareTeam] ok");
            return ""+alarm.getAlNo();
        } else{
            log.info("[shareTeam] login-again");
            return "login-again";
        }
    }

    /***
     * 동행의 권한 상태 변경 (권한상태변경 + 알람서비스)
     * @param map -> teamLevel은 무조건 - 읽기권한:0, 수정권한:1
     * @return
     */
    // http://localhost:8080/teams/share/1  : teamMid=member?teamLevel=0
    @PatchMapping("/teams/share/{scNo}")
    @ResponseBody
    public String updateTeamLevel(@PathVariable int scNo,@RequestBody Map<String,Object> map) throws TeamNotFoundException, ScheduleNotFoundException {
        String id = (String)map.get("teamMid");
        int level = Integer.parseInt(String.valueOf(map.get("teamLevel"))); //String으로 변환한 후 Integer.parseInt
        teamScheduleService.modifyTeamLevel(scNo,id,level);
        if(level==0){ //읽기
            alarmService.addAlarmByUpdateTeamLevelRead(id,scNo);//알람서비스
        } else if(level==1){ //수정
            alarmService.addAlarmByUpdateTeamLevelWrite(id,scNo);//알람서비스
        }
        return "updateTeamLevel ok";
    }

    /**
     * 동행 삭제 (동행 삭제 + 알람서비스)
     * */
    // http://localhost:8080/teams/share/4 : teamMid:jhla456@naver.com
    @DeleteMapping ("/teams/share/{scNo}")
    @ResponseBody
    public String deleteMyTeam(@PathVariable int scNo,@RequestBody Map<String,String> map) throws ScheduleNotFoundException, TeamNotFoundException {
        String id = map.get("teamMid");
        teamScheduleService.removeTeam(scNo, id);
        alarmService.addAlarmByDeleteTeam(scNo,id); //알람서비스
        return "deleteMyTeam ok";
    }

    /**
     * 동행에게 초대 이메일 발송
     */
    // http://localhost:8080/teams/share/send-link/57  : teamId:jhla456@naver.com
    @PostMapping("/teams/share/send-link/{scNo}")
    @ResponseBody
    public String sendCode(@PathVariable int scNo, @RequestBody Map<String, String> map, HttpSession session) {
        String loginId = (String) session.getAttribute("loginId"); //초대하는 호스트 아이디
        String teamMid = map.get("teamMid"); //메세지 보낼 동행 아이디(이메일)

        String subject = ms.getMessage(
                "mail.share-code.subject",
                new Object[]{loginId},
                null);

        String message = ms.getMessage(
                "mail.share-code.message",
                new Object[]{"http://localhost:8080/teams/share/confirm/"+scNo+"/"+teamMid},
                null);

        EmailMessage emailMessage = EmailMessage.builder()
                .recipient(teamMid)
                .subject(subject)
                .message(message)
                .build();

        //이메일 전송
        emailSend.sendMail(emailMessage);
        log.info("email id={}", teamMid);
        log.info("subject={}", subject);
        log.info("message={}", message);
        return "OK";
    }

    /**
     * 이메일 받은 동행이 링크 클릭할 시 이동될 페이지 - teamShareConfirm.html
     * @param scNo
     * @param memId
     * @param model
     */
    //http://localhost:8080/teams/share/confirm/111/jhla456@kakao.com
    @GetMapping("/teams/share/confirm/{scNo}/{memId}")
    public String showTeamShareAuth(@PathVariable int scNo, @PathVariable String memId, Model model) {
        model.addAttribute("memId", memId);
        model.addAttribute("scNo", scNo);
        return "teamShareConfirm";
    }
}
