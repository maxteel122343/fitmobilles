package com.example.domain.fitness

enum class ExerciseCategory(val label: String) {
    FEATURED("Principais"),
    LOWER_BODY("Pernas & Glúteos"),
    UPPER_PUSH("Peito & Empurrar"),
    UPPER_PULL_ARMS("Costas & Braços"),
    SHOULDERS("Ombros & Trapézio"),
    CORE_MOBILITY("Core & Abdômen"),
    MOBILITY_YOGA("Mobilidade & Yoga"),
    FUNCTIONAL_KETTLEBELL("Funcional & Kettlebell"),
    FULL_BODY("Cardio & HIIT")
}

enum class TrackingDriver {
    SQUAT_KNEE,
    PUSHUP_ELBOW,
    CURL_ELBOW,
    LUNGE_KNEE,
    HIP_HINGE,
    SHOULDER_PRESS,
    LATERAL_RAISE,
    CORE_FLEXION
}

data class ExerciseDefinition(
    val id: String,
    val namePt: String,
    val nameEn: String,
    val category: ExerciseCategory,
    val driver: TrackingDriver,
    val targetMuscles: String,
    val downThresholdDeg: Float,
    val upThresholdDeg: Float,
    val primaryCue: String,
    val isFeaturedMvp: Boolean = false,
    val equipment: String = "Peso Corporal",
    val difficulty: String = "Intermediário",
    val stepByStep: List<String> = emptyList(),
    val commonMistake: String = "Evite balançar o tronco ou perder o alinhamento articular."
) {
    fun resolvedSteps(): List<String> = stepByStep.ifEmpty {
        listOf(
            "1. Posicione-se com base firme, coluna neutra e olhar à frente.",
            "2. Execute o movimento controlando até ~${downThresholdDeg.toInt()}° mantendo respiração constante.",
            "3. Retorne com firmeza até ~${upThresholdDeg.toInt()}° sem compensar com a lombar."
        )
    }
}

object ExerciseCatalog {

    val FEATURED_MVP_IDS = listOf("squat", "push_up", "bicep_curl", "lunge")

    val ALL_EXERCISES: List<ExerciseDefinition> by lazy {
        val base = buildCatalog()
        val extra = ExerciseCatalogExtra.buildExtraExercises(base.map { it.id }.toSet())
        base + extra
    }

    val BASIC_INCLUDED_IDS: Set<String> by lazy {
        ALL_EXERCISES.take(40).map { it.id }.toSet()
    }

    fun isIncludedInBasic(id: String): Boolean = id in BASIC_INCLUDED_IDS

    fun findById(id: String): ExerciseDefinition =
        ALL_EXERCISES.firstOrNull { it.id == id } ?: ALL_EXERCISES.first()

    private fun buildCatalog(): List<ExerciseDefinition> {
        val list = mutableListOf<ExerciseDefinition>()

        // 4 Featured MVP exercises (Good-GYM core set)
        list += ExerciseDefinition(
            id = "squat",
            namePt = "Agachamento Livre",
            nameEn = "Squat",
            category = ExerciseCategory.FEATURED,
            driver = TrackingDriver.SQUAT_KNEE,
            targetMuscles = "Quadríceps, Glúteos, Core",
            downThresholdDeg = 98f,
            upThresholdDeg = 158f,
            primaryCue = "Desça até o quadril alinhar com os joelhos mantendo o peito erguido.",
            isFeaturedMvp = true
        )
        list += ExerciseDefinition(
            id = "push_up",
            namePt = "Flexão de Braço",
            nameEn = "Push-up",
            category = ExerciseCategory.FEATURED,
            driver = TrackingDriver.PUSHUP_ELBOW,
            targetMuscles = "Peitoral, Tríceps, Ombros, Core",
            downThresholdDeg = 95f,
            upThresholdDeg = 155f,
            primaryCue = "Mantenha o corpo alinhado em prancha e desça o peito próximo ao solo.",
            isFeaturedMvp = true
        )
        list += ExerciseDefinition(
            id = "bicep_curl",
            namePt = "Rosca Direta",
            nameEn = "Bicep Curl",
            category = ExerciseCategory.FEATURED,
            driver = TrackingDriver.CURL_ELBOW,
            targetMuscles = "Bíceps Braquial, Antebraço",
            downThresholdDeg = 55f,
            upThresholdDeg = 148f,
            primaryCue = "Mantenha os cotovelos fixos ao lado do tronco sem balançar os ombros.",
            isFeaturedMvp = true
        )
        list += ExerciseDefinition(
            id = "lunge",
            namePt = "Afundo / Passada",
            nameEn = "Lunge",
            category = ExerciseCategory.FEATURED,
            driver = TrackingDriver.LUNGE_KNEE,
            targetMuscles = "Quadríceps, Glúteos, Equilíbrio",
            downThresholdDeg = 100f,
            upThresholdDeg = 155f,
            primaryCue = "Mantenha o tronco vertical e forme ângulo de 90° nos dois joelhos.",
            isFeaturedMvp = true
        )

        // 20 Lower Body variations
        val lowerSpecs = listOf(
            Triple("sumo_squat", "Agachamento Sumô", "Adutores e Glúteos"),
            Triple("goblet_squat", "Agachamento Goblet", "Quadríceps e Core"),
            Triple("jump_squat", "Agachamento com Salto", "Potência de Inferiores"),
            Triple("bulgarian_split_squat", "Agachamento Búlgaro", "Glúteo e Quadríceps Unilateral"),
            Triple("front_squat", "Agachamento Frontal", "Quadríceps e Postura"),
            Triple("pistol_squat_assist", "Agachamento Unilateral Assistido", "Força Unilateral"),
            Triple("wall_sit_pulse", "Agachamento Isométrico Pulso", "Resistência de Quadríceps"),
            Triple("box_squat", "Agachamento no Banco", "Controle Excêntrico"),
            Triple("narrow_squat", "Agachamento Base Fechada", "Vasto Lateral"),
            Triple("cossack_squat", "Agachamento Cossaco", "Mobilidade de Quadril e Adutores"),
            Triple("reverse_lunge", "Afundo Reverso", "Glúteos e Posteriores"),
            Triple("walking_lunge", "Passada Caminhando", "Resistência de Pernas"),
            Triple("curtsy_lunge", "Afundo Cruzado", "Glúteo Médio"),
            Triple("lateral_lunge", "Afundo Lateral", "Adutores e Glúteo Médio"),
            Triple("step_up", "Subida no Degrau", "Quadríceps e Glúteos"),
            Triple("calf_raise", "Panturrilha em Pé", "Gastrocnêmio e Sóleo"),
            Triple("single_leg_calf_raise", "Panturrilha Unilateral", "Estabilidade de Tornozelo"),
            Triple("squat_to_calf_raise", "Agachamento + Panturrilha", "Cadeia Inferior Completa"),
            Triple("pulse_lunge", "Afundo com Pulso", "Tensão Contínua em Glúteo"),
            Triple("heel_elevated_squat", "Agachamento Calcanhar Elevado", "Ênfase em Quadríceps")
        )
        lowerSpecs.forEachIndexed { idx, (id, name, muscles) ->
            val isLunge = id.contains("lunge") || id.contains("step")
            list += ExerciseDefinition(
                id = id,
                namePt = name,
                nameEn = name,
                category = ExerciseCategory.LOWER_BODY,
                driver = if (isLunge) TrackingDriver.LUNGE_KNEE else TrackingDriver.SQUAT_KNEE,
                targetMuscles = muscles,
                downThresholdDeg = if (idx % 2 == 0) 98f else 102f,
                upThresholdDeg = 156f,
                primaryCue = "Controle a descida por 2 segundos e empurre o chão pelos calcanhares."
            )
        }

        // 16 Hip Hinge / Posterior Chain
        val hingeSpecs = listOf(
            Triple("romanian_deadlift", "Stiff / Levantamento Romeno", "Posteriores e Glúteos"),
            Triple("good_morning", "Bom Dia (Good Morning)", "Lombar e Isquiotibiais"),
            Triple("glute_bridge", "Elevação Pélvica", "Glúteo Máximo"),
            Triple("single_leg_rdl", "Stiff Unilateral", "Equilíbrio e Posteriores"),
            Triple("kettlebell_swing", "Swing com Peso", "Cadeia Posterior Explosiva"),
            Triple("hip_hinge_drill", "Educativo de Dobradiça de Quadril", "Padrão de Movimento"),
            Triple("frog_pump", "Frog Pump", "Glúteos"),
            Triple("sumo_deadlift", "Terra Sumô", "Glúteos e Adutores"),
            Triple("staggered_rdl", "Stiff Base Alternada", "Isquiotibiais"),
            Triple("hip_thrust_bodyweight", "Hip Thrust Corporal", "Glúteos e Core"),
            Triple("back_extension_standing", "Extensão Lombar em Pé", "Eretores da Espinha"),
            Triple("nordic_hinge_assist", "Flexão Nórdica Assistida", "Isquiotibiais"),
            Triple("deadlift_clean_pull", "Puxada Alta de Terra", "Posterior e Trapézio"),
            Triple("broad_jump_prep", "Salto Horizontal Controlado", "Potência de Quadril"),
            Triple("single_leg_bridge", "Ponte Unilateral", "Estabilidade Pélvica"),
            Triple("tabletop_bridge", "Ponte Mesa Invertida", "Glúteos e Ombros")
        )
        hingeSpecs.forEach { (id, name, muscles) ->
            list += ExerciseDefinition(
                id = id,
                namePt = name,
                nameEn = name,
                category = ExerciseCategory.LOWER_BODY,
                driver = TrackingDriver.HIP_HINGE,
                targetMuscles = muscles,
                downThresholdDeg = 105f,
                upThresholdDeg = 162f,
                primaryCue = "Empurre o quadril para trás mantendo a coluna neutra."
            )
        }

        // 20 Upper Push (Push-ups & Shoulder Presses)
        val pushSpecs = listOf(
            Triple("wide_push_up", "Flexão Aberta", "Peitoral Maior"),
            Triple("diamond_push_up", "Flexão Diamante", "Tríceps e Peitoral Interno"),
            Triple("incline_push_up", "Flexão Inclinada", "Peitoral Inferior"),
            Triple("decline_push_up", "Flexão Declinada", "Peitoral Superior"),
            Triple("knee_push_up", "Flexão com Joelhos", "Peitoral e Tríceps (Iniciante)"),
            Triple("pike_push_up", "Flexão Pike (Ombros)", "Deltoides Anterior e Médio"),
            Triple("tricep_dip_bench", "Mergulho no Banco", "Tríceps Braquial"),
            Triple("archer_push_up", "Flexão Arqueiro", "Peitoral Unilateral"),
            Triple("tempo_push_up", "Flexão Cadenciada 3-1-1", "Controle Neuromuscular"),
            Triple("shoulder_tap_push_up", "Flexão + Toque no Ombro", "Peitoral e Anti-Rotação"),
            Triple("overhead_press", "Desenvolvimento Acima da Cabeça", "Deltoides e Tríceps"),
            Triple("arnold_press", "Desenvolvimento Arnold", "Deltoides Completo"),
            Triple("push_press", "Push Press", "Ombros e Potência"),
            Triple("single_arm_press", "Desenvolvimento Unilateral", "Ombros e Core"),
            Triple("lateral_raise", "Elevação Lateral", "Deltoide Lateral"),
            Triple("front_raise", "Elevação Frontal", "Deltoide Anterior"),
            Triple("scaption_raise", "Elevação em Y (Scaption)", "Manguito e Deltoide"),
            Triple("upright_row", "Remada Alta", "Deltoide e Trapézio"),
            Triple("wall_angel", "Anjo na Parede (Mobilidade)", "Escápulas e Ombros"),
            Triple("overhead_tricep_ext", "Tríceps Francês", "Cabeça Longa do Tríceps")
        )
        pushSpecs.forEach { (id, name, muscles) ->
            val driver = when {
                id.contains("lateral") || id.contains("front_raise") || id.contains("scaption") || id.contains("angel") ->
                    TrackingDriver.LATERAL_RAISE
                id.contains("press") || id.contains("tricep_ext") || id.contains("upright") ->
                    TrackingDriver.SHOULDER_PRESS
                else -> TrackingDriver.PUSHUP_ELBOW
            }
            list += ExerciseDefinition(
                id = id,
                namePt = name,
                nameEn = name,
                category = ExerciseCategory.UPPER_PUSH,
                driver = driver,
                targetMuscles = muscles,
                downThresholdDeg = if (driver == TrackingDriver.LATERAL_RAISE) 28f else 95f,
                upThresholdDeg = if (driver == TrackingDriver.LATERAL_RAISE) 80f else 155f,
                primaryCue = "Mantenha ombros encaixados longe das orelhas e controle a volta."
            )
        }

        // 18 Upper Pull & Arms
        val pullSpecs = listOf(
            Triple("hammer_curl", "Rosca Martelo", "Braquiorradial e Bíceps"),
            Triple("alternating_curl", "Rosca Alternada", "Bíceps Braquial"),
            Triple("concentration_curl", "Rosca Concentrada", "Pico do Bíceps"),
            Triple("wide_bicep_curl", "Rosca Aberta", "Cabeça Curta do Bíceps"),
            Triple("zottman_curl", "Rosca Zottman", "Bíceps e Antebraço"),
            Triple("bent_over_row", "Remada Curvada", "Dorsais e Romboides"),
            Triple("single_arm_row", "Remada Serrote Unilateral", "Latíssimo do Dorso"),
            Triple("reverse_fly", "Crucifixo Inverso", "Deltoide Posterior"),
            Triple("face_pull_band", "Face Pull com Elástico", "Postura e Manguito"),
            Triple("superman_pull", "Superman com Remada", "Dorsais e Lombar"),
            Triple("tricep_kickback", "Tríceps Coice", "Tríceps Braquial"),
            Triple("band_pull_apart", "Abertura de Elástico", "Romboides e Trapézio Médio"),
            Triple("high_pull", "Puxada Alta Dinâmica", "Trapézio e Ombros"),
            Triple("prone_y_raise", "Elevação Y Pronada", "Trapézio Inferior"),
            Triple("prone_t_raise", "Elevação T Pronada", "Escápulas"),
            Triple("shoulder_shrug", "Encolhimento de Ombros", "Trapézio Superior"),
            Triple("isometric_curl_hold", "Rosca Isométrica + Rep", "Resistência de Bíceps"),
            Triple("renegade_row", "Remada Renegada", "Costas e Core")
        )
        pullSpecs.forEach { (id, name, muscles) ->
            val driver = if (id.contains("fly") || id.contains("apart") || id.contains("raise")) {
                TrackingDriver.LATERAL_RAISE
            } else {
                TrackingDriver.CURL_ELBOW
            }
            list += ExerciseDefinition(
                id = id,
                namePt = name,
                nameEn = name,
                category = ExerciseCategory.UPPER_PULL_ARMS,
                driver = driver,
                targetMuscles = muscles,
                downThresholdDeg = 55f,
                upThresholdDeg = 148f,
                primaryCue = "Puxe conduzindo pelos cotovelos e contraia por 1 segundo no topo."
            )
        }

        // 12 Core & Mobility
        val coreSpecs = listOf(
            Triple("standing_knee_drive", "Elevação de Joelho no Core", "Abdômen Inferior e Flexores"),
            Triple("bicycle_crunch", "Abdominal Bicicleta", "Oblíquos e Reto Abdominal"),
            Triple("crunch_basic", "Abdominal Supra", "Reto Abdominal"),
            Triple("russian_twist", "Torção Russa", "Oblíquos"),
            Triple("mountain_climber", "Alpinista (Mountain Climber)", "Core e Cardio"),
            Triple("plank_hip_dip", "Prancha com Rotação de Quadril", "Oblíquos e Transverso"),
            Triple("dead_bug", "Dead Bug Alternado", "Estabilidade Lombar"),
            Triple("bird_dog", "Perdigueiro (Bird Dog)", "Core e Equilíbrio"),
            Triple("standing_side_bend", "Flexão Lateral do Tronco", "Oblíquos"),
            Triple("cat_cow_mobility", "Gato-Camelo (Mobilidade)", "Coluna Torácica e Lombar"),
            Triple("torso_rotation_flow", "Rotação Torácica em Pé", "Mobilidade Espinhal"),
            Triple("hollow_body_tuck", "Hollow Hold com Flexão", "Core Profundo")
        )
        coreSpecs.forEach { (id, name, muscles) ->
            list += ExerciseDefinition(
                id = id,
                namePt = name,
                nameEn = name,
                category = ExerciseCategory.CORE_MOBILITY,
                driver = TrackingDriver.CORE_FLEXION,
                targetMuscles = muscles,
                downThresholdDeg = 85f,
                upThresholdDeg = 145f,
                primaryCue = "Ative o abdômen expirando na contração sem puxar o pescoço."
            )
        }

        // 10 Cardio & Full Body
        val fullSpecs = listOf(
            Triple("jumping_jack", "Polichinelo", "Cardio e Ombros"),
            Triple("squat_to_press", "Thruster (Agacha + Desenvolve)", "Corpo Todo"),
            Triple("burpee_modified", "Burpee Adaptado", "Condicionamento Global"),
            Triple("high_knees", "Corrida Estacionária (Joelho Alto)", "Cardio e Core"),
            Triple("shadow_boxing_jab", "Sombra de Boxe (Jab-Direto)", "Ombros e Cardio"),
            Triple("squat_front_kick", "Agachamento + Chute Frontal", "Pernas e Mobilidade"),
            Triple("inchworm_walkout", "Minhoca (Inchworm)", "Posterior, Core e Ombros"),
            Triple("star_jump", "Salto Estrela", "Potência e Cardio"),
            Triple("lunge_to_curl", "Afundo + Rosca", "Pernas e Bíceps"),
            Triple("squat_side_leg_lift", "Agachamento + Abdução", "Quadríceps e Glúteo Médio")
        )
        fullSpecs.forEach { (id, name, muscles) ->
            val driver = when {
                id.contains("jumping_jack") || id.contains("star") -> TrackingDriver.LATERAL_RAISE
                id.contains("press") -> TrackingDriver.SHOULDER_PRESS
                id.contains("boxing") -> TrackingDriver.PUSHUP_ELBOW
                else -> TrackingDriver.SQUAT_KNEE
            }
            list += ExerciseDefinition(
                id = id,
                namePt = name,
                nameEn = name,
                category = ExerciseCategory.FULL_BODY,
                driver = driver,
                targetMuscles = muscles,
                downThresholdDeg = 95f,
                upThresholdDeg = 152f,
                primaryCue = "Mantenha ritmo constante e respiração fluida durante toda a série."
            )
        }

        return list // Total: 4 + 20 + 16 + 20 + 18 + 12 + 10 = 100 exercises
    }
}
