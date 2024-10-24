package org.firstinspires.ftc.teamcode.p2p

import android.annotation.SuppressLint
import org.firstinspires.ftc.teamcode.Algorithms.PIDF
import org.firstinspires.ftc.teamcode.Algorithms.quality_of_life_funcs.angDiff
import org.firstinspires.ftc.teamcode.Algorithms.quality_of_life_funcs.autoupdate_tp
import org.firstinspires.ftc.teamcode.Autonomous.Pose
import org.firstinspires.ftc.teamcode.Pathing.Trajectory
import org.firstinspires.ftc.teamcode.Variables.PIDCOEF
import org.firstinspires.ftc.teamcode.Variables.PIDCoefs.pidcoefAngle
import org.firstinspires.ftc.teamcode.Variables.PIDCoefs.pidcoefSpeed
import org.firstinspires.ftc.teamcode.Variables.system_funcs.drivetrain
import org.firstinspires.ftc.teamcode.Variables.system_funcs.imew
import org.firstinspires.ftc.teamcode.Variables.system_funcs.localizer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class p2p {
    lateinit var traj: Trajectory
    var lookahead = Pose()
    var a: Double = 0.0
    val someconstant: Double = 0.001
    var haveTraj: Boolean = false
    var done: Boolean = false
    var error: Boolean = false
    var speedpid = PIDF(pidcoefSpeed)
    var turnpid = PIDF(pidcoefAngle)
    var strafepid = PIDF(PIDCOEF(0.05, 0.0, 0.0, 0.02))
    val tolerance: Double = 7.0
    val angtolerance: Double = 0.1

    var busy: Boolean = false

    fun initpurepursuit(traj_real: Trajectory) {
        traj = traj_real
        a = 0.0
        lookahead = traj.startpos
        autoupdate_tp("TRAJSTART", "${traj.startpos.x} ${traj.startpos.y} ${traj.startpos.heading}")
        autoupdate_tp("TRAJEND", "${traj.endpos.x} ${traj.endpos.y} ${traj.endpos.heading}")
    }

    fun isincircle(lookahead: Pose, robotPos: Pose, radius: Double): Boolean {
        autoupdate_tp("DISTANCE BETWEEN LOOKAHEAD AND ROBOTPOS", "${(lookahead - robotPos).distance()}")
        autoupdate_tp("ISINCIRCLE", "${(lookahead - robotPos).distance() < radius}")
        return (lookahead - robotPos).distance() < radius
    }

    @SuppressLint("DefaultLocale")
    fun update() {

        val robotPos = localizer.robotpose
        val err = traj.endpos - robotPos

        lookahead = traj.endpos

        val forwardsError: Double =
            cos(imew.yaw) * (lookahead.x - robotPos.x) - sin(imew.yaw) * (lookahead.y - robotPos.y)
        val strafeError: Double =
            sin(imew.yaw) * (lookahead.x - robotPos.x) + cos(imew.yaw) * (lookahead.y - robotPos.y)
        val headingError: Double =
            angDiff(lookahead.heading, imew.yaw)

        val fwdpwr = speedpid.update(forwardsError) * .5
        val strafepwr = strafepid.update(strafeError) * .5
        val headingpwr = turnpid.update(headingError)


        drivetrain.perurobotcentricdrive(fwdpwr, strafepwr, headingpwr)

        autoupdate_tp("X DISTANCE", String.format("%.4f", lookahead.x - robotPos.x))
        autoupdate_tp("Y DISTANCE", String.format("%.4f", lookahead.y - robotPos.y))
        autoupdate_tp("ROBOTPOSX", String.format("%.4f", robotPos.x))
        autoupdate_tp("ROBOTPOSY", String.format("%.4f", robotPos.y))

        autoupdate_tp("BUSY", busy)

        if (err.distance() < tolerance && abs(headingError) < angtolerance) {
            haveTraj = false
            done = true
            busy = false
        }

        autoupdate_tp("isintolerace",err.distance() < tolerance)
        autoupdate_tp("isinangtolerance", abs(headingError) < angtolerance)
    }

    fun followtraj(t: Trajectory){
        haveTraj = true
        done = false
        busy = true
        initpurepursuit(t)
    }
}