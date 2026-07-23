-- V2: Chuyển đổi callback_data từ TEXT sang JSONB để đồng bộ với mapping của JPA/Hibernate
ALTER TABLE payments ALTER COLUMN callback_data TYPE jsonb USING callback_data::jsonb;
