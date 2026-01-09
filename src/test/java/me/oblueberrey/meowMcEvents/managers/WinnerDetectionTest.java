package me.oblueberrey.meowMcEvents.managers;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for winner detection logic in team-based and solo events.
 * These tests verify the core game logic for determining event winners.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Winner Detection Tests")
class WinnerDetectionTest {

    private TeamManager teamManager;

    @Mock private Player player1;
    @Mock private Player player2;
    @Mock private Player player3;
    @Mock private Player player4;
    @Mock private Player player5;
    @Mock private Player player6;

    private UUID uuid1, uuid2, uuid3, uuid4, uuid5, uuid6;

    @BeforeEach
    void setUp() {
        teamManager = new TeamManager();

        uuid1 = UUID.randomUUID();
        uuid2 = UUID.randomUUID();
        uuid3 = UUID.randomUUID();
        uuid4 = UUID.randomUUID();
        uuid5 = UUID.randomUUID();
        uuid6 = UUID.randomUUID();

        when(player1.getUniqueId()).thenReturn(uuid1);
        when(player2.getUniqueId()).thenReturn(uuid2);
        when(player3.getUniqueId()).thenReturn(uuid3);
        when(player4.getUniqueId()).thenReturn(uuid4);
        when(player5.getUniqueId()).thenReturn(uuid5);
        when(player6.getUniqueId()).thenReturn(uuid6);
    }

    @Nested
    @DisplayName("Solo Mode (Free-for-all) Winner Detection")
    class SoloModeTests {

        @Test
        @DisplayName("Should identify winner when only one player remains alive")
        void soloMode_OnePlayerAlive_ShouldBeWinner() {
            // In solo mode (team size 1), each player is their own team
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 1);

            // Simulate deaths - only player1 survives
            Set<UUID> alivePlayers = new HashSet<>(Collections.singletonList(uuid1));

            // Verify only 1 team (player) is alive
            assertEquals(1, teamManager.getAliveTeamCount(alivePlayers));

            // Verify the winning team is player1's team
            int winningTeam = teamManager.getWinningTeam(alivePlayers);
            assertEquals(teamManager.getTeam(player1), winningTeam);
        }

        @Test
        @DisplayName("Should not have winner when multiple players alive")
        void soloMode_MultiplePlayers_NoWinnerYet() {
            List<Player> players = Arrays.asList(player1, player2, player3);
            teamManager.assignTeams(players, 1);

            Set<UUID> alivePlayers = new HashSet<>(Arrays.asList(uuid1, uuid2));

            // 2 players alive = 2 teams alive
            assertEquals(2, teamManager.getAliveTeamCount(alivePlayers));
        }

        @Test
        @DisplayName("Should handle draw scenario (no players alive)")
        void soloMode_NoPlayersAlive_DrawScenario() {
            List<Player> players = Arrays.asList(player1, player2);
            teamManager.assignTeams(players, 1);

            Set<UUID> alivePlayers = new HashSet<>();

            assertEquals(0, teamManager.getAliveTeamCount(alivePlayers));
            assertEquals(-1, teamManager.getWinningTeam(alivePlayers));
        }
    }

    @Nested
    @DisplayName("Team Mode (2v2) Winner Detection")
    class TeamMode2v2Tests {

        @Test
        @DisplayName("Should identify winning team when opposing team eliminated")
        void teamMode2v2_OneTeamEliminated_OtherTeamWins() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 2);

            // Find which team player1 is on
            int team1Number = teamManager.getTeam(player1);
            Set<UUID> team1Members = teamManager.getTeamMembers(team1Number);

            // Only team1 members are alive
            Set<UUID> alivePlayers = new HashSet<>(team1Members);

            assertEquals(1, teamManager.getAliveTeamCount(alivePlayers));
            assertEquals(team1Number, teamManager.getWinningTeam(alivePlayers));
        }

        @Test
        @DisplayName("Should not declare winner when both teams have alive members")
        void teamMode2v2_BothTeamsAlive_NoWinner() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 2);

            // All players alive
            Set<UUID> alivePlayers = new HashSet<>(Arrays.asList(uuid1, uuid2, uuid3, uuid4));

            assertEquals(2, teamManager.getAliveTeamCount(alivePlayers));
        }

        @Test
        @DisplayName("Should still count team alive if at least one member survives")
        void teamMode2v2_PartialTeamAlive_TeamStillCounted() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 2);

            // Get teammates
            int team1Number = teamManager.getTeam(player1);
            int team2Number = teamManager.getTeam(player3);

            // Ensure we have players from different teams
            if (team1Number == team2Number) {
                team2Number = teamManager.getTeam(player4);
            }

            // One player from each team alive
            UUID team1Survivor = null;
            UUID team2Survivor = null;

            for (UUID uuid : teamManager.getTeamMembers(team1Number)) {
                team1Survivor = uuid;
                break;
            }
            for (UUID uuid : teamManager.getTeamMembers(team2Number)) {
                team2Survivor = uuid;
                break;
            }

            if (team1Survivor != null && team2Survivor != null) {
                Set<UUID> alivePlayers = new HashSet<>(Arrays.asList(team1Survivor, team2Survivor));
                assertEquals(2, teamManager.getAliveTeamCount(alivePlayers));
            }
        }
    }

    @Nested
    @DisplayName("Team Mode (3v3) Winner Detection")
    class TeamMode3v3Tests {

        @Test
        @DisplayName("Should correctly track larger team elimination")
        void teamMode3v3_TeamElimination() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4, player5, player6);
            teamManager.assignTeams(players, 3);

            // Get team 1 members
            int team1Number = teamManager.getTeam(player1);
            Set<UUID> team1Members = teamManager.getTeamMembers(team1Number);

            // Only team 1 survives with 2 out of 3 members
            Set<UUID> survivors = new HashSet<>();
            int count = 0;
            for (UUID member : team1Members) {
                if (count < 2) {
                    survivors.add(member);
                    count++;
                }
            }

            // Team 1 should still be alive
            assertTrue(teamManager.isTeamAlive(team1Number, survivors));
            assertEquals(1, teamManager.getAliveTeamCount(survivors));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle player disconnect during event")
        void playerDisconnect_ShouldUpdateTeamStatus() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 2);

            int team1Number = teamManager.getTeam(player1);
            Set<UUID> originalTeam1 = new HashSet<>(teamManager.getTeamMembers(team1Number));

            // Player disconnects
            teamManager.removeFromTeam(player1);

            // Team should have fewer members but still exist if teammate alive
            Set<UUID> currentTeam1 = teamManager.getTeamMembers(team1Number);
            assertTrue(currentTeam1.size() < originalTeam1.size() || currentTeam1.isEmpty());
        }

        @Test
        @DisplayName("Should handle uneven team sizes correctly")
        void unevenTeams_ShouldStillDetectWinner() {
            // 5 players with team size 2 = teams of 2, 2, and 1
            List<Player> players = Arrays.asList(player1, player2, player3, player4, player5);
            teamManager.assignTeams(players, 2);

            // Find the solo player's team
            int soloTeamNumber = -1;
            for (int teamNum : teamManager.getAllTeamNumbers()) {
                if (teamManager.getTeamMembers(teamNum).size() == 1) {
                    soloTeamNumber = teamNum;
                    break;
                }
            }

            if (soloTeamNumber != -1) {
                Set<UUID> soloTeamMembers = teamManager.getTeamMembers(soloTeamNumber);
                Set<UUID> alivePlayers = new HashSet<>(soloTeamMembers);

                // Solo team can still win
                assertEquals(1, teamManager.getAliveTeamCount(alivePlayers));
                assertEquals(soloTeamNumber, teamManager.getWinningTeam(alivePlayers));
            }
        }

        @Test
        @DisplayName("Should handle rapid elimination sequence")
        void rapidElimination_ShouldTrackCorrectly() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 1); // Solo mode

            Set<UUID> alivePlayers = new HashSet<>(Arrays.asList(uuid1, uuid2, uuid3, uuid4));

            // Rapid eliminations
            alivePlayers.remove(uuid4);
            assertEquals(3, teamManager.getAliveTeamCount(alivePlayers));

            alivePlayers.remove(uuid3);
            assertEquals(2, teamManager.getAliveTeamCount(alivePlayers));

            alivePlayers.remove(uuid2);
            assertEquals(1, teamManager.getAliveTeamCount(alivePlayers));

            // Winner should be player1
            assertEquals(teamManager.getTeam(uuid1), teamManager.getWinningTeam(alivePlayers));
        }

        @Test
        @DisplayName("Should handle simultaneous deaths (mutual kill)")
        void simultaneousDeaths_ShouldResultInDraw() {
            List<Player> players = Arrays.asList(player1, player2);
            teamManager.assignTeams(players, 1);

            // Both players die simultaneously
            Set<UUID> alivePlayers = new HashSet<>();

            assertEquals(0, teamManager.getAliveTeamCount(alivePlayers));
            assertEquals(-1, teamManager.getWinningTeam(alivePlayers));
        }
    }

    @Nested
    @DisplayName("Team Balance Tests")
    class TeamBalanceTests {

        @Test
        @DisplayName("Teams should be balanced when player count is divisible")
        void evenDistribution_TeamsShouldBeBalanced() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4);
            teamManager.assignTeams(players, 2);

            for (int teamNum : teamManager.getAllTeamNumbers()) {
                assertEquals(2, teamManager.getTeamMembers(teamNum).size());
            }
        }

        @Test
        @DisplayName("No team should have more than teamSize players")
        void maxTeamSize_ShouldNotExceed() {
            List<Player> players = Arrays.asList(player1, player2, player3, player4, player5);
            int teamSize = 2;
            teamManager.assignTeams(players, teamSize);

            for (int teamNum : teamManager.getAllTeamNumbers()) {
                assertTrue(teamManager.getTeamMembers(teamNum).size() <= teamSize);
            }
        }
    }
}
