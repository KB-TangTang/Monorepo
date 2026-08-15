import http from '@/api/http';

export async function fetchCategories() {
    const result = await http.get('/categories');
    return result.categories;
}
