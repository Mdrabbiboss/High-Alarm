package com.programmer.highalarm

import kotlin.random.Random

data class MathProblem(
    val expression: String,
    val result: Int,
    val hint: String? = null
)

object MathChallengeGenerator {
    fun generate(difficulty: Difficulty = Difficulty.Medium): MathProblem {
        return when (difficulty) {
            Difficulty.Easy -> {
                val a = Random.nextInt(10, 50)
                val b = Random.nextInt(10, 50)
                if (Random.nextBoolean()) {
                    MathProblem("$a + $b", a + b)
                } else {
                    val max = maxOf(a, b)
                    val min = minOf(a, b)
                    MathProblem("$max - $min", max - min)
                }
            }
            Difficulty.Medium -> {
                val a = Random.nextInt(5, 20)
                val b = Random.nextInt(5, 20)
                val c = Random.nextInt(2, 15)
                if (Random.nextBoolean()) {
                    MathProblem("($a + $b) * $c", (a + b) * c)
                } else {
                    MathProblem("$a * $b + $c", a * b + c)
                }
            }
            Difficulty.Hard -> {
                // Solve for x: ax + b = c -> x = (c - b) / a
                val a = Random.nextInt(2, 10)
                val x = Random.nextInt(2, 12)
                val b = Random.nextInt(1, 30)
                val c = a * x + b
                MathProblem("${a}x + $b = $c", x, "Find x")
            }
            Difficulty.Extreme -> {
                val a = Random.nextInt(12, 30)
                val b = Random.nextInt(12, 30)
                val c = Random.nextInt(50, 200)
                val d = Random.nextInt(2, 10)
                val res = (a * b) + c - d
                MathProblem("($a * $b) + $c - $d", res)
            }
        }
    }

    enum class Difficulty { Easy, Medium, Hard, Extreme }
}

object AdvancedMathChallengeGenerator {
    fun generate(difficulty: Difficulty = Difficulty.Medium): MathProblem {
        var problem: MathProblem
        var attempts = 0
        do {
            problem = when (difficulty) {
                Difficulty.Easy -> createEasyProblem()
                Difficulty.Medium -> createMediumProblem()
                Difficulty.Hard -> createHardProblem()
                Difficulty.Extreme -> createExtremeProblem()
            }
            attempts++
        } while (attempts < 50 && (problem.result > 200 || problem.result < -50))
        
        return problem
    }

    private fun createEasyProblem(): MathProblem {
        // f(x) = ax + b
        val x = Random.nextInt(2, 10)
        val a = Random.nextInt(2, 8)
        val b = Random.nextInt(-10, 10)
        val res = a * x + b
        return MathProblem("f(x) = ${a}x ${if (b >= 0) "+" else ""}$b", res, "Substitute x = $x")
    }

    private fun createMediumProblem(): MathProblem {
        // f(x) = ax^2 + b
        val x = Random.nextInt(2, 6)
        val a = Random.nextInt(1, 4)
        val b = Random.nextInt(-15, 15)
        val res = a * x * x + b
        return MathProblem("f(x) = ${a}x² ${if (b >= 0) "+" else ""}$b", res, "Substitute x = $x")
    }

    private fun createHardProblem(): MathProblem {
        // f(x,y) = ax + by
        val x = Random.nextInt(2, 8)
        val y = Random.nextInt(2, 8)
        val a = Random.nextInt(2, 6)
        val b = Random.nextInt(2, 6)
        val res = a * x + b * y
        return MathProblem("f(x,y) = ${a}x + ${b}y", res, "x=$x, y=$y")
    }

    private fun createExtremeProblem(): MathProblem {
        // Higher polynomials or multi-step substitution
        val x = Random.nextInt(2, 5)
        val a = Random.nextInt(1, 3)
        val b = Random.nextInt(2, 6)
        val c = Random.nextInt(-20, 20)
        val res = a * x * x * x + b * x + c
        return MathProblem("f(x) = ${a}x³ + ${b}x ${if (c >= 0) "+" else ""}$c", res, "Substitute x = $x")
    }

    enum class Difficulty { Easy, Medium, Hard, Extreme }
}
