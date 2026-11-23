package com.example.domain.fitness

/**
 * Generates 440+ additional real-world fitness, calisthenics, strength, mobility, HIIT,
 * kettlebell, dumbbell, barbell, and rehabilitation exercises (combined with the 100 base
 * exercises for 540+ unique movements in the FitVerso library).
 */
internal object ExerciseCatalogExtra {

    private data class BaseTemplate(
        val idPrefix: String,
        val namePt: String,
        val nameEn: String,
        val category: ExerciseCategory,
        val driver: TrackingDriver,
        val targetMuscles: String,
        val downDeg: Float,
        val upDeg: Float,
        val cue: String,
        val mistake: String
    )

    private data class ModifierSpec(
        val suffixId: String,
        val suffixPt: String,
        val suffixEn: String,
        val equipment: String,
        val difficulty: String
    )

    fun buildExtraExercises(existingIds: Set<String>): List<ExerciseDefinition> {
        val out = mutableListOf<ExerciseDefinition>()
        val seenIds = existingIds.toMutableSet()

        fun addUnique(def: ExerciseDefinition) {
            if (seenIds.add(def.id)) {
                out += def
            }
        }

        // 1. Curated standalone specialty exercises across Yoga, Mobility, Rehab, Calisthenics, Cardio & Functional
        val curated = listOf(
            // Mobilidade & Yoga
            BaseTemplate("worlds_greatest_stretch", "Maior Alongamento do Mundo", "World's Greatest Stretch", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.LUNGE_KNEE, "Quadril, Torácica, Posteriores", 102f, 158f, "Dê um passo longo à frente e gire o tronco abrindo o braço para o teto.", "Arredondar a lombar sem estabilizar o pé da frente."),
            BaseTemplate("hip_90_90_switch", "Troca de Quadril 90/90", "90/90 Hip Switch", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.HIP_HINGE, "Rotadores de Quadril", 95f, 155f, "Mantenha os dois joelhos a 90° e gire os quadris mantendo o tronco longo.", "Deixar os calcanhares escorregarem ou curvar a coluna."),
            BaseTemplate("downward_dog_flow", "Cachorro Olhando para Baixo", "Downward Facing Dog", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.HIP_HINGE, "Ombros, Panturrilha, Cadeia Posterior", 100f, 160f, "Empurre o chão com as mãos e eleve os ísquios em direção ao teto.", "Tensionar os ombros junto às orelhas."),
            BaseTemplate("cobra_thoracic_ext", "Postura da Cobra", "Cobra Stretch", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.PUSHUP_ELBOW, "Extensores Torácicos e Abdômen", 95f, 155f, "Abra o peito suavemente empurrando o solo com cotovelos próximos ao tronco.", "Comprimir a lombar em vez de alongar a coluna torácica."),
            BaseTemplate("thread_the_needle", "Agulha na Linha (Rotação Torácica)", "Thread the Needle", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.CORE_FLEXION, "Coluna Torácica e Escápulas", 85f, 145f, "Deslize o braço por baixo do tronco levando o ombro em direção ao solo.", "Deixar o quadril balançar para os lados."),
            BaseTemplate("pigeon_pose_flow", "Postura do Pombo Dinâmica", "Dynamic Pigeon Pose", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.HIP_HINGE, "Glúteo e Piriforme", 105f, 155f, "Alinhe a pelve para frente e respire fundo descendo o tronco com controle.", "Forçar o joelho da frente em rotação desconfortável."),
            BaseTemplate("ankle_dorsiflexion_rock", "Mobilidade de Tornozelo na Parede", "Ankle Dorsiflexion Rock", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.LUNGE_KNEE, "Tornozelo e Sóleo", 105f, 150f, "Projete o joelho sobre a ponta do pé sem levantar o calcanhar do chão.", "Descolar o calcanhar para ganhar amplitude falsa."),
            BaseTemplate("scapular_cars", "Círculos Escapulares (CARs)", "Scapular CARs", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.LATERAL_RAISE, "Escápulas e Trapézio", 25f, 80f, "Mova apenas as escápulas em elevação, retração, depressão e protração.", "Dobrar os cotovelos durante o círculo."),
            BaseTemplate("deep_squat_thoracic_reach", "Agachamento Profundo + Alcance Torácico", "Deep Squat Thoracic Reach", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.SQUAT_KNEE, "Quadril, Tornozelo e Torácica", 92f, 155f, "No fundo do agachamento, segure o pé oposto e gire um braço para cima.", "Perder o apoio total da planta dos pés."),
            BaseTemplate("sun_salutation_a", "Saudação ao Sol A", "Sun Salutation A", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.HIP_HINGE, "Corpo Inteiro e Respiração", 100f, 165f, "Sincronize cada transição entre extensão, flexão de quadril e prancha com a respiração.", "Apressar as transições sem controle postural."),

            // Cardio & HIIT
            BaseTemplate("sprawl_fast", "Sprawl Explosivo", "Explosive Sprawl", ExerciseCategory.FULL_BODY, TrackingDriver.SQUAT_KNEE, "Cardio, Core e Pernas", 98f, 155f, "Jogue os pés para trás caindo em prancha firme e retorne agachando.", "Deixar o quadril despencar ao tocar os pés atrás."),
            BaseTemplate("skater_hop", "Salto Patinador Lateral", "Lateral Skater Hop", ExerciseCategory.FULL_BODY, TrackingDriver.LUNGE_KNEE, "Glúteo Médio, Potência e Cardio", 102f, 155f, "Salte lateralmente absorvendo o impacto com o joelho alinhado ao pé.", "Deixar o joelho cair para dentro na aterrissagem."),
            BaseTemplate("seal_jack", "Polichinelo Frontal (Seal Jack)", "Seal Jack", ExerciseCategory.FULL_BODY, TrackingDriver.LATERAL_RAISE, "Peitoral, Ombros e Cardio", 28f, 82f, "Abra braços e pernas simultaneamente em ritmo estável e leve nos pés.", "Aterrisar com os joelhos travados."),
            BaseTemplate("tuck_jump", "Salto com Joelho no Peito", "Tuck Jump", ExerciseCategory.FULL_BODY, TrackingDriver.SQUAT_KNEE, "Potência de Pernas e Cardio", 95f, 158f, "Salte puxando os joelhos ao alto e amorteça suavemente na descida.", "Bater os calcanhares duro no solo."),
            BaseTemplate("butt_kicks", "Corrida Calcanhar no Glúteo", "Butt Kicks", ExerciseCategory.FULL_BODY, TrackingDriver.LUNGE_KNEE, "Posteriores e Aquecimento", 100f, 156f, "Mantenha postura alta trazendo os calcanhares rapidamente até os glúteos.", "Inclinar o tronco excessivamente para frente."),
            BaseTemplate("cross_body_mountain_climber", "Alpinista Cruzado", "Cross-Body Mountain Climber", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Oblíquos e Cardio", 85f, 145f, "Traga o joelho em direção ao cotovelo oposto mantendo ombros sobre os punhos.", "Elevar muito o quadril perdendo a prancha."),
            BaseTemplate("shadow_uppercut_combo", "Sombra de Boxe (Uppercut + Cruzado)", "Boxing Uppercut Cross", ExerciseCategory.FULL_BODY, TrackingDriver.CURL_ELBOW, "Ombros, Core e Cardio", 58f, 145f, "Gire o quadril junto aos golpes protegendo o queixo com a outra mão.", "Estender os cotovelos no limite sem controle."),
            BaseTemplate("plyo_lunge_switch", "Afundo com Salto Alternado", "Plyometric Jumping Lunge", ExerciseCategory.LOWER_BODY, TrackingDriver.LUNGE_KNEE, "Quadríceps, Glúteos e Potência", 98f, 155f, "Troque as pernas no ar e aterrisse com tronco ereto e base firme.", "Perder o alinhamento lateral dos joelhos."),
            BaseTemplate("plank_jack", "Prancha com Polichinelo", "Plank Jack", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Core, Ombros e Cardio", 88f, 145f, "Salte abrindo e fechando os pés mantendo o tronco imóvel em prancha.", "Balançar a lombar para cima e para baixo."),
            BaseTemplate("lateral_shuffle_touch", "Deslocamento Lateral + Toque no Solo", "Lateral Shuffle Floor Touch", ExerciseCategory.FULL_BODY, TrackingDriver.SQUAT_KNEE, "Agilidade e Inferiores", 98f, 155f, "Desloque-se rápido lateralmente e agache para tocar o solo com coluna neutra.", "Curvar as costas na hora de tocar o chão."),

            // Calistenia & Força Especializada
            BaseTemplate("pull_up_pronated", "Barra Fixa Pronada", "Pull-Up", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Latíssimo do Dorso e Bíceps", 55f, 155f, "Inicie deprimindo as escápulas e puxe o peito em direção à barra.", "Usar impulso excessivo das pernas (kipping sem controle)."),
            BaseTemplate("chin_up_supinated", "Barra Fixa Supinada (Chin-Up)", "Chin-Up", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Bíceps e Dorsais", 52f, 155f, "Com palmas voltadas para você, suba até o queixo passar a barra.", "Encurtar a descida antes da extensão completa."),
            BaseTemplate("inverted_australian_row", "Remada Australiana na Barra Baixa", "Inverted Row", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Costas Médias e Posterior de Ombro", 58f, 150f, "Mantenha o corpo reto como uma prancha e puxe o peito até a barra.", "Deixar o quadril cair antes dos ombros subirem."),
            BaseTemplate("parallel_bar_dip", "Paralelas (Mergulho Completo)", "Parallel Bar Dip", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Peitoral Inferior e Tríceps", 92f, 160f, "Desça até os ombros ficarem levemente abaixo dos cotovelos e empurre firme.", "Projetar os ombros para frente no fundo do movimento."),
            BaseTemplate("copenhagen_plank", "Prancha Copenhague (Adutores)", "Copenhagen Plank", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Adutores e Oblíquos", 88f, 148f, "Apoie a perna superior no banco e eleve o quadril alinhando tronco e pernas.", "Deixar a pelve girar para trás."),
            BaseTemplate("dragon_flag_tuck", "Dragon Flag Flexionado", "Tuck Dragon Flag", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Reto Abdominal e Core Total", 82f, 145f, "Segure um apoio firme atrás da cabeça e controle a descida do quadril.", "Arquear a lombar ao descer as pernas."),
            BaseTemplate("l_sit_tuck_hold", "L-Sit Agrupado", "Tuck L-Sit", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Abdômen, Flexores e Tríceps", 82f, 142f, "Empurre o solo/paralete para baixo mantendo ombros longe das orelhas.", "Deixar os ombros subirem durante a sustentação."),
            BaseTemplate("hindu_push_up", "Flexão Hindu (Mergulho)", "Hindu Push-Up", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Ombros, Peito e Mobilidade Espinhal", 92f, 158f, "Parta do V invertido, mergulhe o peito rente ao chão e estenda olhando à frente.", "Colapsar os cotovelos para fora sem controle."),
            BaseTemplate("pseudo_planche_push_up", "Flexão Pseudo-Planche", "Pseudo Planche Push-Up", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Deltoide Anterior, Peito e Core", 92f, 156f, "Incline os ombros bem à frente dos punhos e faça a flexão mantendo protração.", "Voltar o corpo para trás ao empurrar."),
            BaseTemplate("sissy_squat_supported", "Sissy Squat Assistido", "Assisted Sissy Squat", ExerciseCategory.LOWER_BODY, TrackingDriver.SQUAT_KNEE, "Quadríceps Isolado", 90f, 158f, "Eleve os calcanhares, projete os joelhos à frente e mantenha tronco e coxas alinhados.", "Flexionar o quadril para frente tirando tensão do quadríceps.")
        )

        curated.forEach { t ->
            addUnique(
                buildDef(
                    id = t.idPrefix,
                    namePt = t.namePt,
                    nameEn = t.nameEn,
                    category = t.category,
                    driver = t.driver,
                    muscles = t.targetMuscles,
                    downDeg = t.downDeg,
                    upDeg = t.upDeg,
                    cue = t.cue,
                    mistake = t.mistake,
                    equipment = "Peso Corporal",
                    difficulty = if (t.category == ExerciseCategory.MOBILITY_YOGA) "Iniciante" else "Intermediário"
                )
            )
        }

        // 2. Comprehensive Strength & Biomechanical Movement Matrix (~420+ real variations)
        // Lower Body & Glutes Matrix (10 bases x 11 variations = 110)
        val legBases = listOf(
            BaseTemplate("back_squat", "Agachamento Costas", "Back Squat", ExerciseCategory.LOWER_BODY, TrackingDriver.SQUAT_KNEE, "Quadríceps, Glúteos e Eretores", 95f, 160f, "Desça controlando o quadril atrás e mantenha joelhos alinhados aos pés.", "Deixar os joelhos fecharem para dentro (valgo dinâmico)."),
            BaseTemplate("split_squat", "Agachamento Dividido (Base Fixa)", "Split Squat", ExerciseCategory.LOWER_BODY, TrackingDriver.LUNGE_KNEE, "Quadríceps e Glúteos", 98f, 156f, "Desça verticalmente levando o joelho de trás próximo ao solo.", "Inclinar o tronco muito à frente sobrecarregando o joelho."),
            BaseTemplate("rdl_hinge", "Levantamento Terra Romeno (Stiff)", "Romanian Deadlift", ExerciseCategory.LOWER_BODY, TrackingDriver.HIP_HINGE, "Isquiotibiais e Glúteos", 104f, 165f, "Mantenha leve flexão de joelho e empurre o quadril para trás até alongar posteriores.", "Arredondar a coluna lombar durante a descida."),
            BaseTemplate("hip_thrust_set", "Elevação Pélvica (Hip Thrust)", "Hip Thrust", ExerciseCategory.LOWER_BODY, TrackingDriver.HIP_HINGE, "Glúteo Máximo e Posteriores", 105f, 168f, "Empurre pelos calcanhares e contraia os glúteos no topo com queixo recolhido.", "Hiperestender a lombar no topo do movimento."),
            BaseTemplate("sumo_stance_squat", "Agachamento Sumô Profundo", "Deep Sumo Squat", ExerciseCategory.LOWER_BODY, TrackingDriver.SQUAT_KNEE, "Adutores, Glúteos e Quadríceps", 96f, 158f, "Ponta dos pés em 45° e joelhos acompanhando exatamente a direção dos dedos.", "Fechar os joelhos durante a subida."),
            BaseTemplate("step_down_ctrl", "Descida Controlada no Step", "Controlled Step-Down", ExerciseCategory.LOWER_BODY, TrackingDriver.LUNGE_KNEE, "Quadríceps e Estabilidade de Joelho", 102f, 156f, "Toque o calcanhar de trás suavemente no chão sem despencar o peso.", "Desabar o quadril lateralmente ao descer."),
            BaseTemplate("reverse_lunge_set", "Afundo Reverso Controlado", "Reverse Lunge", ExerciseCategory.LOWER_BODY, TrackingDriver.LUNGE_KNEE, "Glúteos e Quadríceps", 98f, 156f, "Dê um passo largo atrás e suba empurrando pelo calcanhar da perna da frente.", "Dar um passo curto demais apertando o joelho."),
            BaseTemplate("good_morning_hinge", "Good Morning Biomecânico", "Good Morning", ExerciseCategory.LOWER_BODY, TrackingDriver.HIP_HINGE, "Posteriores, Glúteos e Lombar", 106f, 165f, "Coluna neutra e peito aberto enquanto o quadril recua.", "Dobrar demais os joelhos transformando em agachamento."),
            BaseTemplate("lateral_lunge_set", "Afundo Lateral Profundo", "Lateral Lunge", ExerciseCategory.LOWER_BODY, TrackingDriver.LUNGE_KNEE, "Adutores, Glúteo Médio e Quadríceps", 100f, 158f, "Mantenha a perna de apoio estendida e sente sobre o quadril da perna flexionada.", "Girar os pés ou levantar o calcanhar da perna dobrada."),
            BaseTemplate("calf_raise_set", "Elevação de Panturrilha", "Calf Raise", ExerciseCategory.LOWER_BODY, TrackingDriver.SQUAT_KNEE, "Gastrocnêmio e Sóleo", 115f, 168f, "Suba até a ponta máxima dos pés, segure 1s e alongue devagar na descida.", "Quicar rápido sem pausa em cima ou embaixo.")
        )
        val legMods = listOf(
            ModifierSpec("bw", "Peso Corporal", "Bodyweight", "Peso Corporal", "Iniciante"),
            ModifierSpec("db", "com Halteres", "Dumbbell", "Halteres", "Iniciante"),
            ModifierSpec("bb", "com Barra", "Barbell", "Barra", "Intermediário"),
            ModifierSpec("kb", "com Kettlebell", "Kettlebell", "Kettlebell", "Intermediário"),
            ModifierSpec("band", "com Elástico", "Resistance Band", "Elástico", "Iniciante"),
            ModifierSpec("pause3s", "com Pausa 3s", "3s Pause", "Peso Corporal / Barra", "Avançado"),
            ModifierSpec("tempo411", "Cadência 4-1-1", "4-1-1 Tempo", "Halteres", "Intermediário"),
            ModifierSpec("unilateral", "Unilateral", "Single-Leg", "Peso Corporal / Halter", "Avançado"),
            ModifierSpec("pulse", "com Pulso no Fundo", "Bottom Pulse", "Peso Corporal", "Intermediário"),
            ModifierSpec("elevated", "com Déficit / Elevado", "Deficit / Elevated", "Step / Anilha", "Avançado"),
            ModifierSpec("iso_hold", "Isometria + Repetição", "Iso Hold + Rep", "Peso Corporal", "Intermediário")
        )
        for (base in legBases) {
            for (mod in legMods) {
                addUnique(
                    buildDef(
                        id = "${base.idPrefix}_${mod.suffixId}",
                        namePt = "${base.namePt} ${mod.suffixPt}",
                        nameEn = "${mod.suffixEn} ${base.nameEn}",
                        category = base.category,
                        driver = base.driver,
                        muscles = base.targetMuscles,
                        downDeg = base.downDeg,
                        upDeg = base.upDeg,
                        cue = base.cue,
                        mistake = base.mistake,
                        equipment = mod.equipment,
                        difficulty = mod.difficulty
                    )
                )
            }
        }

        // Upper Push & Chest & Shoulders Matrix (9 bases x 10 variations = 90)
        val pushBases = listOf(
            BaseTemplate("chest_press", "Supino Reto", "Flat Chest Press", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Peitoral, Tríceps e Deltoide Anterior", 92f, 158f, "Mantenha escápulas retraídas e desça com cotovelos em ~45° em relação ao tronco.", "Abrir os cotovelos a 90° sobrecarregando o ombro."),
            BaseTemplate("incline_press", "Supino Inclinado", "Incline Press", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Peitoral Superior e Ombros", 92f, 158f, "Direcione a carga acima da linha superior do peito com punhos neutros.", "Descolar a parte superior das costas do apoio."),
            BaseTemplate("floor_press", "Supino no Solo (Floor Press)", "Floor Press", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Peitoral e Tríceps (Proteção de Ombro)", 95f, 160f, "Toque os tríceps suavemente no chão por 1 segundo e empurre com potência.", "Bater os cotovelos com força no piso."),
            BaseTemplate("chest_fly", "Crucifixo Peitoral", "Chest Fly", ExerciseCategory.UPPER_PUSH, TrackingDriver.LATERAL_RAISE, "Peitoral Maior", 26f, 82f, "Mantenha leve dobra fixa nos cotovelos como se abraçasse uma árvore.", "Dobrar e estender os cotovelos transformando em supino."),
            BaseTemplate("overhead_sh_press", "Desenvolvimento de Ombros", "Overhead Shoulder Press", ExerciseCategory.SHOULDERS, TrackingDriver.SHOULDER_PRESS, "Deltoides Anterior/Médio e Tríceps", 92f, 160f, "Contraia abdômen e glúteos para não arquear a lombar ao empurrar acima da cabeça.", "Projetar a cabeça para trás e hiperestender a lombar."),
            BaseTemplate("lat_raise_set", "Elevação Lateral", "Lateral Raise", ExerciseCategory.SHOULDERS, TrackingDriver.LATERAL_RAISE, "Deltoide Lateral", 24f, 82f, "Eleve os braços até a altura dos ombros conduzindo pelos cotovelos.", "Encolher o trapézio ou balançar o tronco."),
            BaseTemplate("front_raise_set", "Elevação Frontal", "Front Raise", ExerciseCategory.SHOULDERS, TrackingDriver.LATERAL_RAISE, "Deltoide Anterior", 25f, 84f, "Suba com controle até a linha dos olhos sem usar impulso da lombar.", "Jogar o quadril para frente para levantar o peso."),
            BaseTemplate("pushup_variation", "Flexão de Braço", "Push-Up", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Peitoral, Tríceps e Core", 94f, 156f, "Corpo rígido da cabeça aos calcanhares, descendo o peito até perto do solo.", "Deixar o quadril cair ou abrir os cotovelos demais."),
            BaseTemplate("tricep_ext_set", "Extensão de Tríceps", "Tricep Extension", ExerciseCategory.UPPER_PUSH, TrackingDriver.PUSHUP_ELBOW, "Tríceps Braquial", 88f, 160f, "Mantenha os braços fixos e mova apenas os antebraços estendendo por completo.", "Mover os cotovelos para frente e para trás.")
        )
        val pushMods = listOf(
            ModifierSpec("db", "com Halteres", "Dumbbell", "Halteres", "Iniciante"),
            ModifierSpec("bb", "com Barra", "Barbell", "Barra", "Intermediário"),
            ModifierSpec("band", "com Elástico", "Band", "Elástico", "Iniciante"),
            ModifierSpec("kb", "com Kettlebell", "Kettlebell", "Kettlebell", "Intermediário"),
            ModifierSpec("alt", "Alternado", "Alternating", "Halteres", "Iniciante"),
            ModifierSpec("uni", "Unilateral", "Single-Arm", "Halter / Cabo", "Intermediário"),
            ModifierSpec("tempo", "Excêntrico Lento 3s", "Slow Eccentric", "Halteres", "Intermediário"),
            ModifierSpec("pause", "com Pausa no Pico", "Peak Pause", "Halteres / Barra", "Avançado"),
            ModifierSpec("neutral", "Pegada Neutra", "Neutral Grip", "Halteres", "Iniciante"),
            ModifierSpec("seated", "Sentado", "Seated", "Banco + Halteres", "Iniciante")
        )
        for (base in pushBases) {
            for (mod in pushMods) {
                addUnique(
                    buildDef(
                        id = "${base.idPrefix}_${mod.suffixId}",
                        namePt = "${base.namePt} ${mod.suffixPt}",
                        nameEn = "${mod.suffixEn} ${base.nameEn}",
                        category = base.category,
                        driver = base.driver,
                        muscles = base.targetMuscles,
                        downDeg = base.downDeg,
                        upDeg = base.upDeg,
                        cue = base.cue,
                        mistake = base.mistake,
                        equipment = mod.equipment,
                        difficulty = mod.difficulty
                    )
                )
            }
        }

        // Upper Pull, Back & Biceps Matrix (8 bases x 10 variations = 80)
        val pullBases = listOf(
            BaseTemplate("bent_row_set", "Remada Curvada", "Bent-Over Row", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Dorsais, Romboides e Trapézio Médio", 60f, 150f, "Incline o tronco com coluna reta e puxe os cotovelos rente ao quadril.", "Arredondar a lombar ou dar trancos com o tronco."),
            BaseTemplate("pulldown_set", "Puxada Frontal", "Lat Pulldown", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Latíssimo do Dorso e Redondo Maior", 58f, 152f, "Abaixe os ombros primeiro e puxe em direção à parte superior do peito.", "Puxar atrás da nuca ou balançar excessivamente."),
            BaseTemplate("bicep_curl_set", "Rosca Bíceps", "Bicep Curl", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Bíceps Braquial", 54f, 150f, "Cotovelos travados ao lado das costelas, suba girando o punho com controle.", "Jogar os cotovelos para frente no início da subida."),
            BaseTemplate("hammer_curl_set", "Rosca Martelo", "Hammer Curl", ExerciseCategory.UPPER_PULL_ARMS, TrackingDriver.CURL_ELBOW, "Braquial, Braquiorradial e Bíceps", 55f, 150f, "Mantenha pegada neutra (polegar para cima) em todo o arco.", "Girar os punhos ou usar impulso das costas."),
            BaseTemplate("rear_delt_fly_set", "Crucifixo Inverso", "Reverse Delt Fly", ExerciseCategory.SHOULDERS, TrackingDriver.LATERAL_RAISE, "Deltoide Posterior e Romboides", 26f, 82f, "Tronco inclinado a 45°, abra os braços apertando as escápulas atrás.", "Subir o tronco durante a abertura."),
            BaseTemplate("face_pull_set", "Face Pull", "Face Pull", ExerciseCategory.SHOULDERS, TrackingDriver.CURL_ELBOW, "Manguito Rotador, Deltoide Posterior e Trapézio", 62f, 148f, "Puxe em direção à testa rodando os polegares para trás no final.", "Levar a cabeça para frente até as mãos."),
            BaseTemplate("upright_row_set", "Remada Alta", "Upright Row", ExerciseCategory.SHOULDERS, TrackingDriver.SHOULDER_PRESS, "Deltoide Lateral e Trapézio", 88f, 152f, "Pegada na largura dos ombros, suba os cotovelos até a altura do peito.", "Pegada muito fechada gerando pinçamento no ombro."),
            BaseTemplate("shrug_set", "Encolhimento de Trapézio", "Shoulder Shrug", ExerciseCategory.SHOULDERS, TrackingDriver.LATERAL_RAISE, "Trapézio Superior", 20f, 65f, "Eleve os ombros direto para as orelhas, segure 2s e desça devagar.", "Fazer círculos com os ombros sob carga.")
        )
        val pullMods = listOf(
            ModifierSpec("db", "com Halteres", "Dumbbell", "Halteres", "Iniciante"),
            ModifierSpec("bb", "com Barra", "Barbell", "Barra", "Intermediário"),
            ModifierSpec("band", "com Elástico", "Band", "Elástico", "Iniciante"),
            ModifierSpec("kb", "com Kettlebell", "Kettlebell", "Kettlebell", "Intermediário"),
            ModifierSpec("cable", "na Polia / Cabo", "Cable", "Polia / Cabo", "Intermediário"),
            ModifierSpec("uni", "Unilateral", "Single-Arm", "Halter / Cabo", "Intermediário"),
            ModifierSpec("alt", "Alternada", "Alternating", "Halteres", "Iniciante"),
            ModifierSpec("incline", "no Banco Inclinado", "Incline Bench", "Banco + Halteres", "Intermediário"),
            ModifierSpec("pause", "com Pausa Isométrica 2s", "2s Iso Pause", "Halteres / Elástico", "Avançado"),
            ModifierSpec("tempo", "Excêntrico Lento", "Slow Eccentric", "Halteres", "Intermediário")
        )
        for (base in pullBases) {
            for (mod in pullMods) {
                addUnique(
                    buildDef(
                        id = "${base.idPrefix}_${mod.suffixId}",
                        namePt = "${base.namePt} ${mod.suffixPt}",
                        nameEn = "${mod.suffixEn} ${base.nameEn}",
                        category = base.category,
                        driver = base.driver,
                        muscles = base.targetMuscles,
                        downDeg = base.downDeg,
                        upDeg = base.upDeg,
                        cue = base.cue,
                        mistake = base.mistake,
                        equipment = mod.equipment,
                        difficulty = mod.difficulty
                    )
                )
            }
        }

        // Core, Functional, Kettlebell & Rehab Matrix (14 bases x 10 variations = 140)
        val coreFuncBases = listOf(
            BaseTemplate("plank_var", "Prancha Abdominal", "Plank", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Transverso, Reto Abdominal e Ombros", 88f, 148f, "Ative glúteos e abdômen em linha reta sem deixar a lombar ceder.", "Elevar demais o quadril ou prender a respiração."),
            BaseTemplate("side_plank_var", "Prancha Lateral", "Side Plank", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Oblíquos e Quadrado Lombar", 88f, 148f, "Alinhe cotovelo abaixo do ombro e mantenha o quadril alto.", "Girar o peito em direção ao chão."),
            BaseTemplate("crunch_var", "Abdominal Supra", "Abdominal Crunch", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Reto Abdominal", 84f, 144f, "Enrole a coluna torácica expirando forte enquanto ergue as escápulas.", "Puxar a cabeça com as mãos tensionando o pescoço."),
            BaseTemplate("russian_twist_var", "Torção Russa", "Russian Twist", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Oblíquos e Core Rotacional", 86f, 144f, "Gire toda a caixa torácica mantendo o peito aberto a 45°.", "Mover apenas os braços de um lado ao outro."),
            BaseTemplate("v_up_var", "Abdominal Canivete (V-Up)", "V-Up", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Abdômen Completo e Flexores", 80f, 145f, "Eleve tronco e pernas juntos formando um V controlado.", "Usar impulso balístico batendo a lombar no solo."),
            BaseTemplate("woodchop_var", "Lenhadouro (Woodchopper)", "Woodchopper", ExerciseCategory.FUNCTIONAL_KETTLEBELL, TrackingDriver.CORE_FLEXION, "Oblíquos, Ombros e Quadril", 85f, 148f, "Gire os pés e o quadril conduzindo a diagonal pelo tronco.", "Fazer força apenas nos braços com coluna torta."),
            BaseTemplate("pallof_var", "Press Pallof Anti-Rotação", "Pallof Press", ExerciseCategory.CORE_MOBILITY, TrackingDriver.PUSHUP_ELBOW, "Core Profundo e Estabilidade", 95f, 160f, "Empurre as mãos à frente do peito resistindo à força lateral.", "Deixar o tronco rodar para o lado da resistência."),
            BaseTemplate("kb_swing_var", "Swing", "Kettlebell Swing", ExerciseCategory.FUNCTIONAL_KETTLEBELL, TrackingDriver.HIP_HINGE, "Glúteos, Posteriores e Potência", 104f, 166f, "Impulsione o peso pela explosão do quadril, braços apenas conduzem.", "Agachar no swing ou levantar o peso com os ombros."),
            BaseTemplate("kb_clean_press", "Clean & Press", "Clean & Press", ExerciseCategory.FUNCTIONAL_KETTLEBELL, TrackingDriver.SHOULDER_PRESS, "Corpo Inteiro e Ombros", 92f, 160f, "Puxe próximo ao tronco encaixando o punho neutro antes de empurrar acima.", "Bater o peso com força no antebraço."),
            BaseTemplate("thruster_var", "Thruster Completo", "Thruster", ExerciseCategory.FUNCTIONAL_KETTLEBELL, TrackingDriver.SQUAT_KNEE, "Pernas, Ombros e Cardio", 94f, 160f, "Aproveite o embalo da subida do agachamento para empurrar acima da cabeça.", "Empurrar antes de estender joelhos e quadris."),
            BaseTemplate("burpee_var", "Burpee Funcional", "Burpee", ExerciseCategory.FULL_BODY, TrackingDriver.SQUAT_KNEE, "Condicionamento Total", 95f, 156f, "Agache, apoie as mãos firme, salte para prancha e retorne alinhado.", "Deixar o quadril bater no solo ao descer."),
            BaseTemplate("jumping_jack_var", "Polichinelo", "Jumping Jack", ExerciseCategory.FULL_BODY, TrackingDriver.LATERAL_RAISE, "Cardio e Coordenação", 28f, 82f, "Aterrissagem macia na ponta dos pés com ritmo respiratório constante.", "Ombros tensos e braços semiflexionados sem amplitude."),
            BaseTemplate("bird_dog_var", "Perdigueiro (Bird-Dog)", "Bird-Dog", ExerciseCategory.MOBILITY_YOGA, TrackingDriver.CORE_FLEXION, "Estabilizadores Lombares e Glúteo", 88f, 148f, "Estenda braço e perna opostos como se empurrasse duas paredes.", "Rotacionar a pelve ou arquear a lombar ao estender."),
            BaseTemplate("dead_bug_var", "Dead Bug (Inseto Morto)", "Dead Bug", ExerciseCategory.CORE_MOBILITY, TrackingDriver.CORE_FLEXION, "Core Profundo e Controle Pélvico", 85f, 145f, "Mantenha a lombar gentilmente colada ao colchonete ao baixar os membros.", "Descolar a lombar do solo formando arco.")
        )
        val coreMods = listOf(
            ModifierSpec("std", "Clássico", "Classic", "Peso Corporal", "Iniciante"),
            ModifierSpec("db", "com Halter", "Dumbbell", "Halteres", "Intermediário"),
            ModifierSpec("kb", "com Kettlebell", "Kettlebell", "Kettlebell", "Intermediário"),
            ModifierSpec("band", "com Elástico", "Band", "Elástico", "Iniciante"),
            ModifierSpec("mb", "com Medicine Ball", "Medicine Ball", "Medicine Ball", "Intermediário"),
            ModifierSpec("alt", "Alternado", "Alternating", "Peso Corporal", "Iniciante"),
            ModifierSpec("uni", "Unilateral", "Single-Side", "Kettlebell / Halter", "Avançado"),
            ModifierSpec("slow", "Controle Lento 3s", "Slow Control", "Peso Corporal", "Intermediário"),
            ModifierSpec("reach", "com Alcance Frontal", "With Reach", "Peso Corporal", "Intermediário"),
            ModifierSpec("tap", "com Toque Alternado", "Alternating Tap", "Peso Corporal", "Iniciante")
        )
        for (base in coreFuncBases) {
            for (mod in coreMods) {
                addUnique(
                    buildDef(
                        id = "${base.idPrefix}_${mod.suffixId}",
                        namePt = "${base.namePt} ${mod.suffixPt}",
                        nameEn = "${mod.suffixEn} ${base.nameEn}",
                        category = base.category,
                        driver = base.driver,
                        muscles = base.targetMuscles,
                        downDeg = base.downDeg,
                        upDeg = base.upDeg,
                        cue = base.cue,
                        mistake = base.mistake,
                        equipment = mod.equipment,
                        difficulty = mod.difficulty
                    )
                )
            }
        }

        return out
    }

    private fun buildDef(
        id: String,
        namePt: String,
        nameEn: String,
        category: ExerciseCategory,
        driver: TrackingDriver,
        muscles: String,
        downDeg: Float,
        upDeg: Float,
        cue: String,
        mistake: String,
        equipment: String,
        difficulty: String
    ): ExerciseDefinition {
        val steps = when (driver) {
            TrackingDriver.SQUAT_KNEE -> listOf(
                "1. Posicione os pés na largura dos ombros com pontas levemente abertas e core ativado.",
                "2. Inicie flexionando quadril e joelhos juntos até atingir ~${downDeg.toInt()}° de dobra no joelho.",
                "3. Empurre o chão pelos calcanhares voltando à extensão completa (~${upDeg.toInt()}°) expirando no esforço."
            )
            TrackingDriver.LUNGE_KNEE -> listOf(
                "1. Estabilize o tronco ereto em base anteroposterior equilibrada.",
                "2. Desça levando os dois joelhos próximos a ~${downDeg.toInt()}° sem deixar o joelho frontal cair para dentro.",
                "3. Suba pressionando pelo meio do pé e calcanhar da perna de trabalho até ~${upDeg.toInt()}°."
            )
            TrackingDriver.HIP_HINGE -> listOf(
                "1. Destrave levemente os joelhos, encaixe as escápulas e mantenha a coluna neutra.",
                "2. Empurre o quadril para trás como se fechasse uma porta até o ângulo alvo (~${downDeg.toInt()}°).",
                "3. Retorne contraindo glúteos e posteriores sem hiperestender a lombar (~${upDeg.toInt()}°)."
            )
            TrackingDriver.PUSHUP_ELBOW -> listOf(
                "1. Alinhe punhos, cotovelos e ombros mantendo escápulas estáveis e abdômen firme.",
                "2. Desça controlando a fase excêntrica por 2 segundos até ~${downDeg.toInt()}° nos cotovelos.",
                "3. Empurre com firmeza até estender os braços próximo a ~${upDeg.toInt()}° sem perder a postura."
            )
            TrackingDriver.CURL_ELBOW -> listOf(
                "1. Fixe os ombros para trás e mantenha os cotovelos estáveis próximos ao tronco.",
                "2. Flexione até o pico de contração (~${downDeg.toInt()}°) apertando o músculo-alvo por 1 segundo.",
                "3. Controle a descida completa até ~${upDeg.toInt()}° sem balançar a lombar."
            )
            TrackingDriver.SHOULDER_PRESS -> listOf(
                "1. Contraia abdômen e glúteos mantendo antebraços verticais na posição inicial.",
                "2. Empurre em linha estável até os braços alinharem acima (~${upDeg.toInt()}°) sem encolher ombros.",
                "3. Retorne devagar até ~${downDeg.toInt()}° preservando controle escapular."
            )
            TrackingDriver.LATERAL_RAISE -> listOf(
                "1. Mantenha postura alta, peito aberto e leve dobra confortável nos cotovelos.",
                "2. Eleve os braços no plano escapular até ~${upDeg.toInt()}° conduzindo pelos cotovelos.",
                "3. Desça em 2 segundos até ~${downDeg.toInt()}° evitando que o peso despenque."
            )
            TrackingDriver.CORE_FLEXION -> listOf(
                "1. Alinhe pelve neutra e ative o abdômen como se preparasse para um leve impacto.",
                "2. Execute a flexão/sustentação expirando o ar com controle (~${downDeg.toInt()}°).",
                "3. Retorne à posição inicial (~${upDeg.toInt()}°) sem soltar a tensão da lombar."
            )
        }

        return ExerciseDefinition(
            id = id,
            namePt = namePt,
            nameEn = nameEn,
            category = category,
            driver = driver,
            targetMuscles = muscles,
            downThresholdDeg = downDeg,
            upThresholdDeg = upDeg,
            primaryCue = cue,
            equipment = equipment,
            difficulty = difficulty,
            stepByStep = steps,
            commonMistake = mistake
        )
    }
}
