package com.index.platform.service;

import com.index.platform.domain.*;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.*;
import com.index.platform.security.AuthUser;
import com.index.platform.util.PhoneUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Several people sharing one shop or one cooperative account. */
@Service
@RequiredArgsConstructor
public class TeamService {
    private final UserRepository users;
    private final OrgMemberRepository members;
    private final ProfileResolver resolver;
    private final PasswordEncoder encoder;

    private record Org(String type, Long id, User owner) {}

    private Org org(AuthUser u) {
        if (u.role() == Role.SHOP) {
            ShopProfile p = resolver.shop(u);
            return new Org("SHOP", p.getId(), p.getUser());
        }
        if (u.role() == Role.FARMER) {
            FarmerProfile p = resolver.farmer(u);
            return new Org("FARMER", p.getId(), p.getUser());
        }
        throw ApiException.forbidden("Only shops and farmers have a team");
    }

    @Transactional(readOnly = true)
    public List<Dto.TeamMember> list(AuthUser u) {
        Org o = org(u);
        List<Dto.TeamMember> out = new ArrayList<>();
        out.add(toMember(o.owner(), "OWNER"));
        for (OrgMember m : members.findByOrgTypeAndOrgId(o.type(), o.id())) {
            users.findById(m.getUserId()).ifPresent(x -> out.add(toMember(x, "MEMBER")));
        }
        return out;
    }

    private Dto.TeamMember toMember(User x, String role) {
        return new Dto.TeamMember(x.getId(), x.getFullName(), x.getEmail(), x.getPhone(), role, x.isActive());
    }

    @Transactional
    public Dto.TeamMember add(AuthUser u, Dto.AddMemberRequest r) {
        Org o = org(u);
        if (!o.owner().getId().equals(u.id())) throw ApiException.forbidden("Only the account owner can add team members");
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email is already registered");
        User m = new User();
        m.setEmail(email);
        m.setPasswordHash(encoder.encode(r.password()));
        m.setFullName(r.fullName().trim());
        m.setPhone(PhoneUtil.normalize(r.phone()));
        m.setRole(u.role());
        m.setLanguage(o.owner().getLanguage());
        m.setApprovalStatus(ApprovalStatus.APPROVED);
        m.setPhoneVerified(true);
        users.save(m);
        OrgMember om = new OrgMember();
        om.setOrgType(o.type()); om.setOrgId(o.id()); om.setUserId(m.getId());
        members.save(om);
        return toMember(m, "MEMBER");
    }

    @Transactional
    public void remove(AuthUser u, Long userId) {
        Org o = org(u);
        if (!o.owner().getId().equals(u.id())) throw ApiException.forbidden("Only the account owner can remove team members");
        OrgMember m = members.findByUserId(userId)
                .filter(x -> x.getOrgType().equals(o.type()) && x.getOrgId().equals(o.id()))
                .orElseThrow(() -> ApiException.notFound("Team member"));
        users.findById(userId).ifPresent(x -> x.setActive(false));
        members.delete(m);
    }
}
