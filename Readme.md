CookMate - ДЗ 5: тестирование

ФИО: Басов Иван Алексеевич  
Группа: Б9123-09.03.03пикд

Проект сделан поверх ДЗ 4 и сохраняет тот же стек: Compose only, Hilt, Retrofit, Room, Navigation Compose, `mutableStateOf`, coroutines и `viewModelScope`.

## Сколько сделано

Юнит-тестов: 20  
Интеграционных Android-тестов: 9  
Нетривиальных тестов: 5+

## Юнит-тесты

1. `CookMateViewModelTest.kt` - 9 тестов:
   - начальное состояние экрана;
   - успешная загрузка;
   - ошибка загрузки;
   - `retrySearch()` после ошибки;
   - пустой результат как `Empty`, а не `Success(emptyList())`;
   - отсутствие дублей в `allMeals`;
   - retry действительно инициирует новый запрос;
   - отмена устаревшего поиска не даёт старому результату перезаписать новый;
   - обновление избранного из Room-наблюдения.

2. `MealRepositoryTest.kt` - 4 теста:
   - корректное преобразование API-модели в доменную модель;
   - пустой ответ API превращается в пустой список;
   - успешная загрузка деталей;
   - ошибка, если детали не найдены.

3. `MealRepositoryFlowTest.kt` - 3 Flow-теста:
   - `repositoryFlow_emitsApiResultThenCompletes()` проверяет полную последовательность эмиссий репозиторного Flow: результат API -> completion;
   - `repositoryFlow_emptyApiResponseEmitsEmptyListThenCompletes()` проверяет полную последовательность для пустого ответа API: `emptyList -> completion`;
   - `flatMapLatest_dropsSlowOldRepositoryResult()` проверяет нетривиальное потоковое поведение: медленный старый запрос отменяется, в UI-цепочку попадает только новый результат.

4. `ViewModelContractTest.kt` - 3 unit/contract-теста:
   - поиск -> загрузка деталей выбранного id;
   - ошибка -> `retrySearch()` -> успешное состояние;
   - корректные переходы состояний `Empty -> Success -> Empty`.

5. `ExampleUnitTest.kt` - 1 базовый системный тест.

## Интеграционные тесты

1. `RoomIntegrationTest.kt` - 4 теста:
   - сохранение и чтение рецепта из Room;
   - повторное добавление не создаёт дубль благодаря primary key;
   - удаление рецепта;
   - сортировка избранного по `addedAt`.

2. `DataLayerIntegrationTest.kt` - 2 теста:
   - запись в DAO и чтение через Room;
   - одно живое наблюдение Room Flow проверяет последовательность эмиссий `one item -> empty` после удаления.

3. `RepositoryRoomIntegrationTest.kt` - 1 тест:
   - связка `Repository + Fake API + Room`: результат API сохраняется в Room и читается обратно.

4. `UiNavigationIntegrationTest.kt` - 1 тест:
   - `SearchScreen -> клик по элементу -> DetailScreen` открывает детали нужного id.

5. `ExampleInstrumentedTest.kt` - 1 базовый Android-тест.

## Покрытые сценарии

- успешная загрузка данных;
- ошибка загрузки;
- retry после ошибки;
- корректное начальное состояние;
- `Loading / Error + Retry / Empty / Success`;
- отсутствие дублей в избранном и списке;
- корректное преобразование моделей;
- обработка пустого результата;
- сохранение и чтение из Room;
- интеграция `Repository + Fake API + Room`;
- UI-навигация список -> детали;
- Flow-последовательности репозитория и Room;
- нетривиальное Flow-поведение с отменой устаревшего запроса.
